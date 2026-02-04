#!/usr/bin/env python3
"""
Script para enriquecer dados de ciclistas usando CyclingRanking.com.

Uso:
    pip install requests beautifulsoup4
    python enrich_from_cyclingranking.py input.csv output.csv

Formato do CSV de entrada (mínimo):
    Nome,Equipa,Ranking
    Tadej Pogačar,UAE Team Emirates,1

O script vai buscar: nacionalidade, idade, especialidade, e calcular o preço.
"""

import csv
import sys
import time
import re
import io
from datetime import datetime
from typing import Optional, Dict, Any, List

# Fix Windows console encoding
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

try:
    import requests
    from bs4 import BeautifulSoup
except ImportError:
    print("A instalar dependências...")
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "requests", "beautifulsoup4"])
    import requests
    from bs4 import BeautifulSoup


# Headers para simular browser
HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
    'Accept-Language': 'en-US,en;q=0.5',
}

# Mapeamento de códigos de país para nomes completos
COUNTRY_MAP = {
    'SLO': 'Slovenia', 'DEN': 'Denmark', 'BEL': 'Belgium', 'NED': 'Netherlands',
    'FRA': 'France', 'ESP': 'Spain', 'ITA': 'Italy', 'GBR': 'United Kingdom',
    'USA': 'United States', 'AUS': 'Australia', 'COL': 'Colombia', 'ECU': 'Ecuador',
    'POR': 'Portugal', 'GER': 'Germany', 'SUI': 'Switzerland', 'AUT': 'Austria',
    'NOR': 'Norway', 'POL': 'Poland', 'CZE': 'Czech Republic', 'IRL': 'Ireland',
    'ERI': 'Eritrea', 'RSA': 'South Africa', 'NZL': 'New Zealand', 'CAN': 'Canada',
    'KAZ': 'Kazakhstan', 'RUS': 'Russia', 'UKR': 'Ukraine', 'LUX': 'Luxembourg',
    'SVK': 'Slovakia', 'HUN': 'Hungary', 'LAT': 'Latvia', 'EST': 'Estonia',
    'LTU': 'Lithuania', 'MEX': 'Mexico', 'ARG': 'Argentina', 'VEN': 'Venezuela',
    'JPN': 'Japan', 'CHN': 'China', 'KOR': 'South Korea', 'GRE': 'Greece',
    'CRO': 'Croatia', 'SRB': 'Serbia', 'ROU': 'Romania', 'BUL': 'Bulgaria',
}


def calculate_age_from_birthday(birthday_str: str) -> Optional[int]:
    """Calcula idade a partir de string de data (ex: '21-Sep-1998')."""
    if not birthday_str:
        return None
    try:
        birth = datetime.strptime(birthday_str, "%d-%b-%Y")
        today = datetime.now()
        age = today.year - birth.year - ((today.month, today.day) < (birth.month, birth.day))
        return age
    except:
        try:
            # Tenta formato alternativo
            birth = datetime.strptime(birthday_str, "%Y-%m-%d")
            today = datetime.now()
            age = today.year - birth.year - ((today.month, today.day) < (birth.month, birth.day))
            return age
        except:
            return None


def calculate_price(ranking: int) -> float:
    """
    Calcula o preço do ciclista baseado no ranking UCI.

    Escala:
    - Top 5: €13-15M
    - Top 10: €10-12M
    - Top 25: €7.5-10M
    - Top 50: €5.5-7.5M
    - Top 100: €4.5-5.5M
    - Top 200: €4-4.5M
    - Resto: €3.5-4M
    """
    if ranking <= 1:
        return 15.0
    elif ranking <= 3:
        return 14.0 - (ranking - 2) * 0.5
    elif ranking <= 5:
        return 13.0 - (ranking - 4) * 0.5
    elif ranking <= 10:
        return 12.0 - (ranking - 6) * 0.4
    elif ranking <= 25:
        return 10.0 - (ranking - 11) * 0.15
    elif ranking <= 50:
        return 7.5 - (ranking - 26) * 0.08
    elif ranking <= 100:
        return 5.5 - (ranking - 51) * 0.02
    elif ranking <= 200:
        return 4.5 - (ranking - 101) * 0.005
    else:
        return 4.0

    return round(max(3.5, min(15.0, price)), 1)


def infer_category_from_team_and_name(name: str, team: str) -> str:
    """
    Tenta inferir a categoria baseado no nome e equipa.
    Usa conhecimento de ciclistas famosos.
    """
    name_lower = name.lower()

    # Sprinters conhecidos
    sprinters = ['philipsen', 'milan', 'groenewegen', 'merlier', 'ackermann',
                 'cavendish', 'jakobsen', 'kooij', 'de lie', 'groves', 'matthews']
    if any(s in name_lower for s in sprinters):
        return "SPRINTER"

    # GC riders conhecidos
    gc_riders = ['pogačar', 'pogacar', 'vingegaard', 'evenepoel', 'roglic', 'roglič',
                 'almeida', 'ayuso', 'rodriguez', 'yates', 'mas', 'carapaz', 'bernal',
                 'hindley', 'vlasov', 'tiberi', 'jorgenson', 'uijtdebroeks', 'thomas']
    if any(g in name_lower for g in gc_riders):
        return "GC"

    # Climbers conhecidos
    climbers = ['healy', 'vine', 'gaudu', 'martinez', 'bilbao', 'landa', 'ciccone',
                'buitrago', 'quintana', 'kuss', 'powless', 'bardet', 'gall']
    if any(c in name_lower for c in climbers):
        return "CLIMBER"

    # Classics specialists
    classics = ['van der poel', 'van aert', 'pidcock', 'pedersen', 'asgreen',
                'mohoric', 'mohorič', 'alaphilippe', 'laporte', 'van baarle', 'benoot']
    if any(c in name_lower for c in classics):
        return "CLASSICS"

    return "ROULEUR"


def search_cyclist_cyclingranking(name: str, team: str) -> Optional[Dict[str, Any]]:
    """
    Busca um ciclista no CyclingRanking.com.

    Retorna dicionário com dados encontrados ou None.
    """
    try:
        # Normaliza o nome para busca
        search_name = name.replace("ž", "z").replace("č", "c").replace("š", "s")
        search_name = search_name.replace("á", "a").replace("é", "e").replace("í", "i")
        search_name = search_name.replace("ó", "o").replace("ú", "u").replace("ñ", "n")

        # Faz a busca
        search_url = f"https://www.cyclingranking.com/riders?q={search_name.replace(' ', '+')}"

        response = requests.get(search_url, headers=HEADERS, timeout=15)
        if response.status_code != 200:
            return None

        soup = BeautifulSoup(response.text, 'html.parser')

        # Procura na tabela de resultados
        rows = soup.select('table tbody tr')

        for row in rows:
            cells = row.select('td')
            if len(cells) >= 3:
                rider_name = cells[0].get_text(strip=True)
                rider_team = cells[1].get_text(strip=True) if len(cells) > 1 else ''
                rider_nat = cells[2].get_text(strip=True) if len(cells) > 2 else ''

                # Verifica se é o ciclista correto
                if name.lower() in rider_name.lower() or rider_name.lower() in name.lower():
                    # Tenta obter mais dados do link do perfil
                    link = row.select_one('a[href*="/rider/"]')
                    birthday = None

                    if link:
                        rider_url = f"https://www.cyclingranking.com{link['href']}"
                        try:
                            rider_response = requests.get(rider_url, headers=HEADERS, timeout=15)
                            if rider_response.status_code == 200:
                                rider_soup = BeautifulSoup(rider_response.text, 'html.parser')
                                # Procura data de nascimento
                                info_items = rider_soup.select('.rider-info li, .info-item')
                                for item in info_items:
                                    text = item.get_text(strip=True)
                                    if 'born' in text.lower() or 'birthday' in text.lower():
                                        # Extrai a data
                                        match = re.search(r'\d{1,2}-\w{3}-\d{4}', text)
                                        if match:
                                            birthday = match.group()
                        except:
                            pass

                    return {
                        'name': rider_name,
                        'nationality': rider_nat,
                        'team': rider_team,
                        'birthday': birthday,
                    }

        return None

    except Exception as e:
        print(f"    Erro na busca: {e}")
        return None


def process_csv(input_file: str, output_file: str):
    """
    Processa o CSV de entrada e gera um CSV enriquecido.
    """
    cyclists = []

    print(f"\n{'='*60}")
    print("Enriquecedor de Dados de Ciclistas - CyclingRanking.com")
    print(f"{'='*60}")
    print(f"\nA ler ficheiro: {input_file}")

    # Lê o CSV de entrada (tenta várias codificações)
    encodings_to_try = ['utf-8', 'utf-8-sig', 'cp1252', 'iso-8859-1', 'latin-1']
    content = None

    for encoding in encodings_to_try:
        try:
            with open(input_file, 'r', encoding=encoding) as f:
                content = f.read()
                break
        except UnicodeDecodeError:
            continue

    if content is None:
        print("ERRO: Não foi possível ler o ficheiro com nenhuma codificação conhecida.")
        return

    # Determina o delimitador
    if ';' in content[:1024]:
        delimiter = ';'
    else:
        delimiter = ','

    # Parse o CSV
    import io
    reader = csv.DictReader(io.StringIO(content), delimiter=delimiter)
    rows = list(reader)

    print(f"Encontrados {len(rows)} ciclistas para processar")
    print(f"Delimitador detectado: '{delimiter}'\n")

    for i, row in enumerate(rows, 1):
        # Limpa espaços nos nomes das colunas
        row = {k.strip(): v for k, v in row.items()}

        # Flexível com nomes de colunas
        name = row.get('Nome', row.get('name', row.get('Name', row.get('nome', ''))))
        team = row.get('Equipa', row.get('team', row.get('Team', row.get('equipa', ''))))
        ranking_str = row.get('UCI Ranking', row.get('Ranking', row.get('ranking', row.get('UCI', row.get('uci_ranking', '')))))
        profile_url = row.get('Link', row.get('URL', row.get('url', row.get('profile_url', ''))))
        nationality = row.get('Nacionalidade', row.get('nationality', row.get('Nationality', '')))

        # Limpa valores
        name = name.strip() if name else ''
        team = team.strip() if team else ''
        nationality = nationality.strip() if nationality else ''
        profile_url = profile_url.strip() if profile_url else ''

        # Remove caracteres especiais do início do nome (BOM, non-breaking spaces, etc.)
        name = name.lstrip('\ufeff\xa0\u00a0 ')

        # Parse ranking (0 = desconhecido, trata como ranking baixo)
        if ranking_str:
            ranking_str = str(ranking_str).strip()
            ranking = int(ranking_str) if ranking_str.isdigit() else 999
            if ranking == 0:
                ranking = 999  # Ranking 0 = desconhecido
        else:
            ranking = 999

        if not name or not team:
            continue

        # Detecta e converte formato "APELIDO Nome" para "Nome Apelido"
        # Ex: "VAN DER POEL Mathieu" -> "Mathieu van der Poel"
        words = name.split()
        if len(words) >= 2:
            # Encontra onde começa o primeiro nome (primeira palavra que não é toda maiúscula)
            first_name_idx = None
            for idx, word in enumerate(words):
                # Se a palavra não é toda maiúscula, é o primeiro nome
                if not word.isupper() and word[0].isupper():
                    first_name_idx = idx
                    break

            if first_name_idx is not None and first_name_idx > 0:
                # Reconstrói: primeiro nome + apelido (título case)
                first_names = ' '.join(words[first_name_idx:])
                last_names = ' '.join(words[:first_name_idx]).title()
                name = f"{first_names} {last_names}"

        # Gera URL se não fornecido
        if not profile_url:
            # Converte nome para slug ProCyclingStats
            import unicodedata
            slug = name.lower()
            slug = unicodedata.normalize('NFD', slug)
            slug = ''.join(c for c in slug if unicodedata.category(c) != 'Mn')
            slug = slug.replace(' ', '-').replace("'", "")
            profile_url = f"rider/{slug}"

        print(f"[{i}/{len(rows)}] {name} ({team})...")

        # Dados base
        cyclist_data = {
            'name': name,
            'team': team,
            'ranking': ranking,
            'nationality': nationality,
            'age': None,
            'speciality': infer_category_from_team_and_name(name, team),
            'category': infer_category_from_team_and_name(name, team),
            'price': calculate_price(ranking),
            'profile_url': profile_url,
        }

        # Tenta buscar dados adicionais (opcional - pode ser lento)
        # Descomenta se quiseres buscar online:
        # fetched = search_cyclist_cyclingranking(name, team)
        # if fetched:
        #     if fetched['nationality']:
        #         cyclist_data['nationality'] = fetched['nationality']
        #     if fetched.get('birthday'):
        #         cyclist_data['age'] = calculate_age_from_birthday(fetched['birthday'])
        #     print(f"  ✓ Online: {cyclist_data['nationality']}")

        print(f"  → {cyclist_data['category']} | €{cyclist_data['price']:.1f}M")
        cyclists.append(cyclist_data)

    # Separa nome em primeiro e último nome
    for c in cyclists:
        name_parts = c['name'].split(' ', 1)
        c['first_name'] = name_parts[0]
        c['last_name'] = name_parts[1] if len(name_parts) > 1 else ''

    # Escreve o CSV de saída
    print(f"\n{'='*60}")
    print(f"A guardar {len(cyclists)} ciclistas em: {output_file}")

    fieldnames = ['first_name', 'last_name', 'team', 'nationality', 'age',
                  'uci_ranking', 'speciality', 'price', 'category', 'profile_url']

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
                'profile_url': c.get('profile_url', ''),
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

    total_value = sum(c['price'] for c in cyclists)
    avg_price = total_value / len(cyclists) if cyclists else 0
    print(f"\nValor total: €{total_value:.1f}M")
    print(f"Preço médio: €{avg_price:.2f}M")


def main():
    if len(sys.argv) < 2:
        print("Uso: python enrich_from_cyclingranking.py input.csv [output.csv]")
        print("\nFormato do CSV de entrada:")
        print("  Nome,Equipa,Ranking")
        print("  Tadej Pogačar,UAE Team Emirates,1")
        print("  Jonas Vingegaard,Team Visma-Lease a Bike,2")
        print("\nSe output.csv não for especificado, usa 'cyclists_enriched.csv'")
        sys.exit(1)

    input_file = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else 'cyclists_enriched.csv'

    process_csv(input_file, output_file)


if __name__ == '__main__':
    main()
