#!/usr/bin/env python3
"""
Script para extrair dados de ciclistas do FirstCycling
usando a biblioteca first_cycling_api.

Uso:
    pip install first_cycling_api
    python extract_from_firstcycling.py

Gera um ficheiro cyclists.csv compativel com a app CiclismoPortugal.
"""

import csv
import sys
import time

# Tenta importar/instalar a biblioteca
try:
    from first_cycling_api import Rider, Team
except ImportError:
    print("A instalar a biblioteca first_cycling_api...")
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "first_cycling_api"])
    from first_cycling_api import Rider, Team


# IDs das equipas WorldTour 2026 no FirstCycling
# Estes IDs podem precisar de ser atualizados
WORLDTOUR_TEAMS = {
    38373: "Alpecin-Premier Tech",
    38374: "Bahrain Victorious",
    38375: "Decathlon-AG2R",
    38376: "EF Education-EasyPost",
    38377: "Groupama-FDJ",
    38378: "INEOS Grenadiers",
    38379: "Lidl-Trek",
    38380: "Lotto-Intermarche",
    38381: "Movistar Team",
    38382: "Red Bull-BORA-hansgrohe",
    38383: "Soudal Quick-Step",
    38384: "Jayco AlUla",
    38385: "Picnic-PostNL",
    38386: "UAE Team Emirates",
    38387: "Uno-X Mobility",
    38388: "Team Visma-Lease a Bike",
    38389: "XDS Astana",
    38390: "NSN Cycling",
}


def get_team_roster(team_id: int, team_name: str) -> list:
    """Extrair roster de uma equipa"""
    cyclists = []

    try:
        print(f"\nA processar {team_name} (ID: {team_id})...")
        team = Team(team_id)

        # Tentar obter o roster
        roster = team.roster()

        if hasattr(roster, 'riders') and roster.riders:
            for rider in roster.riders:
                try:
                    rider_name = rider.get('name', '')
                    nationality = rider.get('nationality', '')

                    # Separar nome
                    name_parts = rider_name.split(' ', 1) if rider_name else ['', '']
                    first_name = name_parts[0] if name_parts else ''
                    last_name = name_parts[1] if len(name_parts) > 1 else ''

                    cyclists.append({
                        'first_name': first_name,
                        'last_name': last_name,
                        'team': team_name,
                        'nationality': nationality,
                        'age': '',
                        'uci_ranking': '',
                        'speciality': '',
                        'price': '5.0',
                        'category': 'ROULEUR'
                    })
                    print(f"  + {rider_name}")
                except Exception as e:
                    continue

    except Exception as e:
        print(f"  Erro: {e}")

    return cyclists


def export_to_csv(cyclists: list, filename: str = 'cyclists_firstcycling.csv'):
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
    print("Extrator FirstCycling - Equipas WorldTour 2026")
    print("=" * 60)

    print("\nNota: Este script tenta usar a API do FirstCycling.")
    print("Se falhar, usa o ficheiro worldtour_2026_riders.csv incluido.\n")

    all_cyclists = []

    for team_id, team_name in WORLDTOUR_TEAMS.items():
        cyclists = get_team_roster(team_id, team_name)
        all_cyclists.extend(cyclists)
        time.sleep(1)  # Rate limiting

    if all_cyclists:
        export_to_csv(all_cyclists)
        print("\n" + "=" * 60)
        print("CONCLUIDO!")
        print(f"Total: {len(all_cyclists)} ciclistas")
        print("=" * 60)
    else:
        print("\n" + "=" * 60)
        print("FALHOU - Usa o ficheiro worldtour_2026_riders.csv")
        print("=" * 60)


if __name__ == '__main__':
    main()
