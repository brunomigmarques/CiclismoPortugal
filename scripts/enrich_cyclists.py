#!/usr/bin/env python3
"""
Script para enriquecer dados de ciclistas usando a biblioteca procyclingstats.

Uso:
    pip install procyclingstats
    python enrich_cyclists.py input.csv output.csv

Formato do CSV de entrada (mínimo):
    Nome,Equipa,Ranking,URL
    Tadej Pogačar,UAE Team Emirates,1,rider/tadej-pogacar

O script vai buscar: nacionalidade, idade, especialidade, e calcular o preço.
"""

import csv
import sys
import time
from datetime import datetime
from typing import Optional, Dict, Any

# Tenta importar/instalar a biblioteca
try:
    from procyclingstats import Rider
except ImportError:
    print("A instalar a biblioteca procyclingstats...")
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "procyclingstats"])
    from procyclingstats import Rider


# Mapeamento de códigos de país para nomes
COUNTRY_CODES = {
    'SLO': 'Slovenia', 'DEN': 'Denmark', 'BEL': 'Belgium', 'NED': 'Netherlands',
    'FRA': 'France', 'ESP': 'Spain', 'ITA': 'Italy', 'GBR': 'United Kingdom',
    'USA': 'United States', 'AUS': 'Australia', 'COL': 'Colombia', 'ECU': 'Ecuador',
    'POR': 'Portugal', 'GER': 'Germany', 'SUI': 'Switzerland', 'AUT': 'Austria',
    'NOR': 'Norway', 'POL': 'Poland', 'CZE': 'Czech Republic', 'IRL': 'Ireland',
    'ERI': 'Eritrea', 'RSA': 'South Africa', 'NZL': 'New Zealand', 'CAN': 'Canada',
    'KAZ': 'Kazakhstan', 'RUS': 'Russia', 'UKR': 'Ukraine', 'LUX': 'Luxembourg',
    'SVK': 'Slovakia', 'HUN': 'Hungary', 'LAT': 'Latvia', 'EST': 'Estonia',
    'LTU': 'Lithuania', 'MEX': 'Mexico', 'ARG': 'Argentina', 'VEN': 'Venezuela',
}


def calculate_age(birthdate: str) -> Optional[int]:
    """Calcula idade a partir da data de nascimento (YYYY-MM-DD)."""
    if not birthdate:
        return None
    try:
        birth = datetime.strptime(birthdate, "%Y-%m-%d")
        today = datetime.now()
        age = today.year - birth.year - ((today.month, today.day) < (birth.month, birth.day))
        return age
    except:
        return None


def calculate_price(ranking: int, speciality_points: Dict[str, int]) -> float:
    """
    Calcula o preço do ciclista baseado no ranking e pontos de especialidade.

    Fórmula:
    - Top 10: 10-15M
    - Top 50: 6-10M
    - Top 100: 4-6M
    - Resto: 3-5M
    """
    if ranking <= 5:
        base_price = 14.0 - (ranking - 1) * 0.5  # 15, 14.5, 14, 13.5, 13
    elif ranking <= 10:
        base_price = 12.0 - (ranking - 6) * 0.4  # ~10-12
    elif ranking <= 25:
        base_price = 9.5 - (ranking - 11) * 0.15  # ~7.5-9.5
    elif ranking <= 50:
        base_price = 7.0 - (ranking - 26) * 0.08  # ~5-7
    elif ranking <= 100:
        base_price = 5.5 - (ranking - 51) * 0.03  # ~4-5.5
    elif ranking <= 200:
        base_price = 4.5 - (ranking - 101) * 0.01  # ~3.5-4.5
    else:
        base_price = 4.0

    # Ajusta baseado em pontos de especialidade
    total_spec_points = sum(speciality_points.values()) if speciality_points else 0
    if total_spec_points > 2000:
        base_price += 1.0
    elif total_spec_points > 1000:
        base_price += 0.5

    return round(max(3.0, min(15.0, base_price)), 1)


def determine_category(speciality_points: Dict[str, int]) -> str:
    """
    Determina a categoria do ciclista baseado nos pontos por especialidade.

    Especialidades do PCS:
    - one-day-races
    - gc (general classification)
    - time-trial
    - sprint
    - climber
    - hills (puncheur)
    """
    if not speciality_points:
        return "ROULEUR"

    # Encontra a especialidade com mais pontos
    max_spec = max(speciality_points, key=speciality_points.get, default=None)

    if max_spec == "sprint":
        return "SPRINTER"
    elif max_spec == "climber":
        return "CLIMBER"
    elif max_spec == "gc":
        return "GC"
    elif max_spec in ["one-day-races", "hills"]:
        return "CLASSICS"
    elif max_spec == "time-trial":
        return "ROULEUR"
    else:
        return "ROULEUR"


def get_speciality_name(speciality_points: Dict[str, int]) -> str:
    """Retorna o nome da especialidade principal."""
    if not speciality_points:
        return ""

    max_spec = max(speciality_points, key=speciality_points.get, default=None)

    spec_names = {
        "sprint": "Sprinter",
        "climber": "Climber",
        "gc": "GC",
        "one-day-races": "Classics",
        "hills": "Puncheur",
        "time-trial": "Time Trial"
    }

    return spec_names.get(max_spec, "All-rounder")


def extract_rider_url(url_or_name: str) -> str:
    """
    Extrai o path do rider a partir de um URL completo ou nome.

    Exemplos:
    - https://www.procyclingstats.com/rider/tadej-pogacar -> rider/tadej-pogacar
    - rider/tadej-pogacar -> rider/tadej-pogacar
    - Tadej Pogačar -> rider/tadej-pogacar (tentativa)
    """
    if "procyclingstats.com" in url_or_name:
        # Extrai o path do URL
        parts = url_or_name.split("procyclingstats.com/")
        if len(parts) > 1:
            return parts[1].strip("/")

    if url_or_name.startswith("rider/"):
        return url_or_name

    # Tenta converter nome para slug
    import unicodedata
    name = url_or_name.lower()
    # Remove acentos
    name = unicodedata.normalize('NFD', name)
    name = ''.join(c for c in name if unicodedata.category(c) != 'Mn')
    # Substitui espaços por hífens
    name = name.replace(" ", "-")
    return f"rider/{name}"


def fetch_rider_data(url_path: str) -> Optional[Dict[str, Any]]:
    """
    Busca dados de um ciclista usando a API procyclingstats.

    Retorna um dicionário com todos os dados ou None se falhar.
    """
    try:
        rider = Rider(url_path)
        data = rider.parse()

        # Extrai dados relevantes
        result = {
            'name': data.get('name', ''),
            'nationality': data.get('nationality', ''),
            'birthdate': data.get('birthdate', ''),
            'weight': data.get('weight'),
            'height': data.get('height'),
            'speciality_points': {},
        }

        # Pontos por especialidade
        spec_points = data.get('points_per_specialty', {})
        if spec_points:
            result['speciality_points'] = spec_points

        return result

    except Exception as e:
        print(f"  Erro ao buscar {url_path}: {e}")
        return None


def process_csv(input_file: str, output_file: str):
    """
    Processa o CSV de entrada e gera um CSV enriquecido.

    Formato de entrada esperado:
    Nome,Equipa,Ranking,URL

    Formato de saída:
    first_name,last_name,team,nationality,age,uci_ranking,speciality,price,category
    """
    cyclists = []

    print(f"\n{'='*60}")
    print("Enriquecedor de Dados de Ciclistas")
    print(f"{'='*60}")
    print(f"\nA ler ficheiro: {input_file}")

    # Lê o CSV de entrada
    with open(input_file, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        rows = list(reader)

    print(f"Encontrados {len(rows)} ciclistas para processar\n")

    for i, row in enumerate(rows, 1):
        name = row.get('Nome', row.get('name', row.get('Name', '')))
        team = row.get('Equipa', row.get('team', row.get('Team', '')))
        ranking_str = row.get('Ranking', row.get('ranking', row.get('UCI', '')))
        url = row.get('URL', row.get('url', row.get('Link', '')))

        ranking = int(ranking_str) if ranking_str and ranking_str.isdigit() else 999

        print(f"[{i}/{len(rows)}] A processar: {name}...")

        # Dados base
        cyclist_data = {
            'name': name,
            'team': team,
            'ranking': ranking,
            'nationality': '',
            'age': None,
            'speciality': '',
            'category': 'ROULEUR',
            'price': 5.0,
        }

        # Tenta buscar dados adicionais
        if url:
            url_path = extract_rider_url(url)
            fetched = fetch_rider_data(url_path)

            if fetched:
                # Atualiza com dados buscados
                if fetched['nationality']:
                    cyclist_data['nationality'] = fetched['nationality']

                if fetched['birthdate']:
                    cyclist_data['age'] = calculate_age(fetched['birthdate'])

                spec_points = fetched.get('speciality_points', {})
                if spec_points:
                    cyclist_data['speciality'] = get_speciality_name(spec_points)
                    cyclist_data['category'] = determine_category(spec_points)
                    cyclist_data['price'] = calculate_price(ranking, spec_points)
                else:
                    cyclist_data['price'] = calculate_price(ranking, {})

                print(f"  ✓ {cyclist_data['nationality']} | {cyclist_data['category']} | €{cyclist_data['price']}M")
            else:
                cyclist_data['price'] = calculate_price(ranking, {})
                print(f"  ✗ Sem dados adicionais, usando defaults")
        else:
            cyclist_data['price'] = calculate_price(ranking, {})
            print(f"  - Sem URL, usando ranking para preço")

        cyclists.append(cyclist_data)

        # Rate limiting para não sobrecarregar o site
        time.sleep(1.5)

    # Separa nome em primeiro e último nome
    for c in cyclists:
        name_parts = c['name'].split(' ', 1)
        c['first_name'] = name_parts[0]
        c['last_name'] = name_parts[1] if len(name_parts) > 1 else ''

    # Escreve o CSV de saída
    print(f"\n{'='*60}")
    print(f"A guardar {len(cyclists)} ciclistas em: {output_file}")

    fieldnames = ['first_name', 'last_name', 'team', 'nationality', 'age',
                  'uci_ranking', 'speciality', 'price', 'category']

    with open(output_file, 'w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()

        for c in cyclists:
            writer.writerow({
                'first_name': c['first_name'],
                'last_name': c['last_name'],
                'team': c['team'],
                'nationality': c['nationality'],
                'age': c['age'] if c['age'] else '',
                'uci_ranking': c['ranking'],
                'speciality': c['speciality'],
                'price': c['price'],
                'category': c['category'],
            })

    print(f"\n{'='*60}")
    print("CONCLUÍDO!")
    print(f"{'='*60}")

    # Estatísticas
    categories = {}
    for c in cyclists:
        cat = c['category']
        categories[cat] = categories.get(cat, 0) + 1

    print("\nEstatísticas por categoria:")
    for cat, count in sorted(categories.items()):
        print(f"  {cat}: {count}")

    avg_price = sum(c['price'] for c in cyclists) / len(cyclists)
    print(f"\nPreço médio: €{avg_price:.2f}M")


def main():
    if len(sys.argv) < 2:
        print("Uso: python enrich_cyclists.py input.csv [output.csv]")
        print("\nFormato do CSV de entrada:")
        print("  Nome,Equipa,Ranking,URL")
        print("  Tadej Pogačar,UAE Team Emirates,1,rider/tadej-pogacar")
        print("\nSe output.csv não for especificado, usa 'cyclists_enriched.csv'")
        sys.exit(1)

    input_file = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else 'cyclists_enriched.csv'

    process_csv(input_file, output_file)


if __name__ == '__main__':
    main()
