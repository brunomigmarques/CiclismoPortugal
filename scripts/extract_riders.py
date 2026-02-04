#!/usr/bin/env python3
"""
Script para extrair dados de ciclistas individuais do ProCyclingStats
e exportar para CSV compativel com a app CiclismoPortugal.

Uso:
    python extract_riders.py

Podes colar uma lista de URLs de ciclistas e o script gera um CSV.
"""

import csv
import sys
import time

try:
    from procyclingstats import Rider
except ImportError:
    print("A instalar a biblioteca procyclingstats...")
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "procyclingstats"])
    from procyclingstats import Rider


def extract_rider(rider_url: str, team_name: str = '') -> dict:
    """Extrair dados de um ciclista"""
    try:
        # Clean URL - extract rider path
        if "procyclingstats.com/" in rider_url:
            rider_path = rider_url.split("procyclingstats.com/")[1]
        else:
            rider_path = rider_url

        rider = Rider(rider_path)
        data = rider.parse()

        # Extract name parts
        name = data.get('name', '')
        name_parts = name.split(' ', 1)
        first_name = name_parts[0] if name_parts else ''
        last_name = name_parts[1] if len(name_parts) > 1 else ''

        # Get team from data if not provided
        if not team_name:
            teams = data.get('teams_history', [])
            if teams:
                team_name = teams[0].get('team_name', '') if isinstance(teams[0], dict) else str(teams[0])

        # Get speciality from points breakdown
        specialities = data.get('points_per_speciality', {})
        main_speciality = ''
        if specialities:
            max_points = 0
            for spec, points in specialities.items():
                if points and points > max_points:
                    max_points = points
                    main_speciality = spec

        # Map speciality to category
        category = 'ROULEUR'
        spec_lower = main_speciality.lower() if main_speciality else ''
        if 'sprint' in spec_lower:
            category = 'SPRINTER'
        elif 'climb' in spec_lower or 'mountain' in spec_lower:
            category = 'CLIMBER'
        elif 'gc' in spec_lower or 'stage' in spec_lower:
            category = 'GC'
        elif 'one_day' in spec_lower or 'classic' in spec_lower:
            category = 'CLASSICS'

        # Calculate price based on ranking (simplified)
        price = 5.0
        ranking = data.get('ranking_position')
        if ranking:
            try:
                rank_num = int(ranking)
                if rank_num <= 10:
                    price = 15.0
                elif rank_num <= 25:
                    price = 12.0
                elif rank_num <= 50:
                    price = 10.0
                elif rank_num <= 100:
                    price = 8.0
                elif rank_num <= 200:
                    price = 6.5
            except:
                pass

        return {
            'first_name': first_name,
            'last_name': last_name,
            'team': team_name,
            'nationality': data.get('nationality', ''),
            'age': data.get('age', ''),
            'uci_ranking': data.get('ranking_position', ''),
            'speciality': main_speciality.replace('_', ' ').title() if main_speciality else '',
            'price': str(price),
            'category': category
        }

    except Exception as e:
        print(f"Erro: {e}")
        return None


def export_to_csv(cyclists: list, filename: str = 'cyclists.csv'):
    """Exportar ciclistas para CSV"""
    if not cyclists:
        print("Nenhum ciclista para exportar!")
        return

    fieldnames = ['first_name', 'last_name', 'team', 'nationality', 'age',
                  'uci_ranking', 'speciality', 'price', 'category']

    with open(filename, 'w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        for cyclist in cyclists:
            writer.writerow(cyclist)

    print(f"\n✓ Exportados {len(cyclists)} ciclistas para {filename}")


def main():
    print("=" * 60)
    print("Extrator de Ciclistas Individuais - ProCyclingStats")
    print("=" * 60)

    # Ask for team name
    team_name = input("\nNome da equipa (opcional, pressiona Enter para ignorar): ").strip()

    print("\nCola os URLs dos ciclistas (um por linha)")
    print("Quando terminares, escreve 'fim' ou deixa em branco e pressiona Enter duas vezes")
    print("\nExemplo:")
    print("  https://www.procyclingstats.com/rider/mathieu-van-der-poel")
    print("  https://www.procyclingstats.com/rider/jasper-philipsen")
    print("-" * 60)

    rider_urls = []
    empty_count = 0

    while True:
        url = input().strip()
        if url.lower() == 'fim':
            break
        if not url:
            empty_count += 1
            if empty_count >= 2:
                break
            continue
        empty_count = 0

        if 'rider/' in url or url.startswith('rider/'):
            rider_urls.append(url)
        # Also accept just the rider name/slug
        elif url and not url.startswith('http'):
            rider_urls.append(f"rider/{url}")

    if not rider_urls:
        print("\nNenhum URL fornecido. A sair...")
        return

    print(f"\n{'=' * 60}")
    print(f"A processar {len(rider_urls)} ciclistas...")
    print("=" * 60)

    cyclists = []
    for i, url in enumerate(rider_urls):
        print(f"[{i+1}/{len(rider_urls)}] {url.split('rider/')[-1]}...", end=' ')

        cyclist = extract_rider(url, team_name)
        if cyclist:
            cyclists.append(cyclist)
            print(f"OK - {cyclist['first_name']} {cyclist['last_name']}")
        else:
            print("FALHOU")

        time.sleep(0.5)  # Rate limiting

    if cyclists:
        export_to_csv(cyclists)
        print("\n" + "=" * 60)
        print("CONCLUIDO!")
        print(f"Ficheiro: cyclists.csv")
        print(f"Total: {len(cyclists)} ciclistas")
        print("\nAgora importa o ficheiro na app CiclismoPortugal")
        print("(Admin Sync > Importar CSV)")
        print("=" * 60)
    else:
        print("\nNenhum ciclista extraido!")


if __name__ == '__main__':
    main()
