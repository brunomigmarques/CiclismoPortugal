#!/usr/bin/env python3
"""
Script para extrair dados de ciclistas do ProCyclingStats
e exportar para CSV compativel com a app CiclismoPortugal.

Uso:
    python extract_cyclists.py

O script vai pedir os URLs das equipas e gerar um ficheiro cyclists.csv
"""

import csv
import sys
import time

try:
    from procyclingstats import Team, Rider
except ImportError:
    print("A instalar a biblioteca procyclingstats...")
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "procyclingstats"])
    from procyclingstats import Team, Rider


def get_rider_details(rider_url: str) -> dict:
    """Obter detalhes de um ciclista individual"""
    try:
        # Remove base URL if present
        if "procyclingstats.com/" in rider_url:
            rider_url = rider_url.split("procyclingstats.com/")[1]

        rider = Rider(rider_url)
        data = rider.parse()

        # Extract name parts
        name = data.get('name', '')
        name_parts = name.split(' ', 1)
        first_name = name_parts[0] if name_parts else ''
        last_name = name_parts[1] if len(name_parts) > 1 else ''

        # Get speciality from points breakdown
        specialities = data.get('points_per_speciality', {})
        main_speciality = ''
        if specialities:
            # Find highest scoring speciality
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

        return {
            'first_name': first_name,
            'last_name': last_name,
            'nationality': data.get('nationality', ''),
            'age': data.get('age', ''),
            'speciality': main_speciality.replace('_', ' ').title() if main_speciality else '',
            'category': category
        }
    except Exception as e:
        print(f"  Erro ao obter detalhes: {e}")
        return {}


def extract_team_cyclists(team_url: str) -> list:
    """Extrair ciclistas de uma equipa"""
    cyclists = []

    try:
        # Clean URL - extract team path
        if "procyclingstats.com/" in team_url:
            team_path = team_url.split("procyclingstats.com/")[1]
        else:
            team_path = team_url

        print(f"\nA processar equipa: {team_path}")

        team = Team(team_path)
        data = team.parse()

        team_name = data.get('name', 'Unknown Team')
        riders = data.get('riders', [])

        print(f"  Equipa: {team_name}")
        print(f"  Encontrados {len(riders)} ciclistas")

        for i, rider in enumerate(riders):
            rider_name = rider.get('name', '')
            rider_url = rider.get('url', '')

            print(f"  [{i+1}/{len(riders)}] {rider_name}...", end=' ')

            # Parse name
            name_parts = rider_name.split(' ', 1) if rider_name else ['', '']
            first_name = name_parts[0] if name_parts else ''
            last_name = name_parts[1] if len(name_parts) > 1 else ''

            cyclist_data = {
                'first_name': first_name,
                'last_name': last_name,
                'team': team_name,
                'nationality': rider.get('nationality', ''),
                'age': rider.get('age', ''),
                'uci_ranking': '',
                'speciality': '',
                'price': '5.0',
                'category': 'ROULEUR'
            }

            # Try to get more details from rider page
            if rider_url:
                time.sleep(0.5)  # Rate limiting
                details = get_rider_details(rider_url)
                if details:
                    cyclist_data.update({
                        'first_name': details.get('first_name', cyclist_data['first_name']),
                        'last_name': details.get('last_name', cyclist_data['last_name']),
                        'nationality': details.get('nationality', cyclist_data['nationality']),
                        'age': details.get('age', cyclist_data['age']),
                        'speciality': details.get('speciality', ''),
                        'category': details.get('category', 'ROULEUR')
                    })

            cyclists.append(cyclist_data)
            print("OK")

    except Exception as e:
        print(f"Erro ao processar equipa: {e}")

    return cyclists


def export_to_csv(cyclists: list, filename: str = 'cyclists.csv'):
    """Exportar ciclistas para CSV"""
    if not cyclists:
        print("Nenhum ciclista para exportar!")
        return

    fieldnames = ['first_name', 'last_name', 'team', 'nationality', 'age',
                  'uci_ranking', 'speciality', 'price', 'category']

    with open(filename, 'w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        # Don't write header - app expects data only
        for cyclist in cyclists:
            writer.writerow(cyclist)

    print(f"\n✓ Exportados {len(cyclists)} ciclistas para {filename}")


def main():
    print("=" * 50)
    print("Extrator de Ciclistas - ProCyclingStats")
    print("=" * 50)
    print("\nIntroduz os URLs das equipas (um por linha)")
    print("Quando terminares, escreve 'fim' ou deixa em branco")
    print("\nExemplo: https://www.procyclingstats.com/team/alpecin-deceuninck-2024")
    print("-" * 50)

    team_urls = []
    while True:
        url = input("\nURL da equipa: ").strip()
        if not url or url.lower() == 'fim':
            break
        if 'team/' in url or url.startswith('team/'):
            team_urls.append(url)
            print(f"  ✓ Adicionada ({len(team_urls)} equipas)")
        else:
            print("  ✗ URL invalido (deve conter 'team/')")

    if not team_urls:
        print("\nNenhuma equipa fornecida. A sair...")
        return

    print(f"\n{'=' * 50}")
    print(f"A processar {len(team_urls)} equipas...")
    print("=" * 50)

    all_cyclists = []
    for url in team_urls:
        cyclists = extract_team_cyclists(url)
        all_cyclists.extend(cyclists)
        time.sleep(1)  # Delay between teams

    if all_cyclists:
        export_to_csv(all_cyclists)
        print("\n" + "=" * 50)
        print("CONCLUIDO!")
        print(f"Ficheiro: cyclists.csv")
        print(f"Total: {len(all_cyclists)} ciclistas")
        print("\nAgora importa o ficheiro na app CiclismoPortugal")
        print("(Admin Sync > Importar CSV)")
        print("=" * 50)
    else:
        print("\nNenhum ciclista extraido!")


if __name__ == '__main__':
    main()
