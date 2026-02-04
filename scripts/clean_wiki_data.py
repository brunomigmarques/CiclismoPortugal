#!/usr/bin/env python3
"""
Clean Wikipedia cyclist data and add team associations
"""

import json
import re
import csv

# Known team patterns to filter out
TEAM_PATTERNS = [
    'premier tech', 'education', 'easypost', 'groupama', 'fdj', 'ineos', 'grenadiers',
    'red bull', 'bora', 'hansgrohe', 'decathlon', 'cma cgm', 'soudal', 'quick-step',
    'visma', 'lease', 'bike', 'movistar', 'lidl', 'trek', 'jayco', 'alula',
    'picnic', 'postnl', 'lotto', 'intermarche', 'bahrain', 'victorious',
    'uno-x', 'mobility', 'uae', 'emirates', 'xds', 'astana', 'nsn', 'cycling',
    'arkea', 'hotels', 'collstrop', 'nardi', 'fassa', 'bortolo', 'saunier', 'duval',
    'prodir', 'san remo', 'lombardia', 'strade bianche', 'hamburg', 'cyclassics',
    'bretagne', 'classic', 'copenhagen', 'sprint', 'ridelondon', 'surrey',
    'alpecin', 'ef education'
]

# Known top cyclists with their data
TOP_CYCLISTS = {
    'Tadej Pogačar': {'team': 'UAE Team Emirates-XRG', 'nationality': 'Slovenia', 'ranking': 1, 'category': 'GC', 'price': 15.0},
    'Jonas Vingegaard': {'team': 'Team Visma-Lease a Bike', 'nationality': 'Denmark', 'ranking': 2, 'category': 'GC', 'price': 14.0},
    'Remco Evenepoel': {'team': 'Soudal Quick-Step', 'nationality': 'Belgium', 'ranking': 3, 'category': 'GC', 'price': 13.0},
    'Primož Roglič': {'team': 'Red Bull-BORA-hansgrohe', 'nationality': 'Slovenia', 'ranking': 4, 'category': 'GC', 'price': 12.0},
    'Jasper Philipsen': {'team': 'Alpecin-Premier Tech', 'nationality': 'Belgium', 'ranking': 7, 'category': 'SPRINTER', 'price': 11.0},
    'Jonathan Milan': {'team': 'Lidl-Trek', 'nationality': 'Italy', 'ranking': 8, 'category': 'SPRINTER', 'price': 10.0},
    'Biniam Girmay': {'team': 'Lidl-Trek', 'nationality': 'Eritrea', 'ranking': 9, 'category': 'SPRINTER', 'price': 10.0},
    'Mads Pedersen': {'team': 'Lidl-Trek', 'nationality': 'Denmark', 'ranking': 10, 'category': 'CLASSICS', 'price': 10.0},
    'João Almeida': {'team': 'UAE Team Emirates-XRG', 'nationality': 'Portugal', 'ranking': 11, 'category': 'GC', 'price': 10.0},
    'Juan Ayuso': {'team': 'UAE Team Emirates-XRG', 'nationality': 'Spain', 'ranking': 12, 'category': 'GC', 'price': 10.0},
    'Adam Yates': {'team': 'UAE Team Emirates-XRG', 'nationality': 'United Kingdom', 'ranking': 20, 'category': 'GC', 'price': 8.0},
    'Matteo Jorgenson': {'team': 'Team Visma-Lease a Bike', 'nationality': 'USA', 'ranking': 13, 'category': 'GC', 'price': 9.5},
    'Richard Carapaz': {'team': 'EF Education-EasyPost', 'nationality': 'Ecuador', 'ranking': 23, 'category': 'GC', 'price': 8.0},
    'Ben Healy': {'team': 'EF Education-EasyPost', 'nationality': 'Ireland', 'ranking': 21, 'category': 'CLIMBER', 'price': 8.0},
    'Tim Merlier': {'team': 'Soudal Quick-Step', 'nationality': 'Belgium', 'ranking': 15, 'category': 'SPRINTER', 'price': 9.0},
    'Mikel Landa': {'team': 'Soudal Quick-Step', 'nationality': 'Spain', 'ranking': 33, 'category': 'CLIMBER', 'price': 7.0},
    'Kasper Asgreen': {'team': 'Soudal Quick-Step', 'nationality': 'Denmark', 'ranking': 36, 'category': 'CLASSICS', 'price': 6.5},
    'Enric Mas': {'team': 'Movistar Team', 'nationality': 'Spain', 'ranking': 29, 'category': 'GC', 'price': 7.5},
    'Cian Uijtdebroeks': {'team': 'Movistar Team', 'nationality': 'Belgium', 'ranking': 31, 'category': 'GC', 'price': 7.0},
    'Nelson Oliveira': {'team': 'Movistar Team', 'nationality': 'Portugal', 'ranking': 70, 'category': 'ROULEUR', 'price': 4.5},
    'Rui Oliveira': {'team': 'UAE Team Emirates-XRG', 'nationality': 'Portugal', 'ranking': 80, 'category': 'ROULEUR', 'price': 4.0},
    'Ivo Oliveira': {'team': 'UAE Team Emirates-XRG', 'nationality': 'Portugal', 'ranking': 85, 'category': 'ROULEUR', 'price': 4.0},
    'Sepp Kuss': {'team': 'Team Visma-Lease a Bike', 'nationality': 'USA', 'ranking': 22, 'category': 'CLIMBER', 'price': 8.0},
    'Christophe Laporte': {'team': 'Team Visma-Lease a Bike', 'nationality': 'France', 'ranking': 16, 'category': 'ROULEUR', 'price': 8.5},
    'Wout van Aert': {'team': 'Team Visma-Lease a Bike', 'nationality': 'Belgium', 'ranking': 6, 'category': 'CLASSICS', 'price': 13.0},
    'Mathieu van der Poel': {'team': 'Alpecin-Premier Tech', 'nationality': 'Netherlands', 'ranking': 5, 'category': 'CLASSICS', 'price': 14.0},
    'Tom Pidcock': {'team': 'INEOS Grenadiers', 'nationality': 'United Kingdom', 'ranking': 17, 'category': 'CLASSICS', 'price': 9.0},
    'Carlos Rodríguez': {'team': 'INEOS Grenadiers', 'nationality': 'Spain', 'ranking': 19, 'category': 'GC', 'price': 8.5},
    'Egan Bernal': {'team': 'INEOS Grenadiers', 'nationality': 'Colombia', 'ranking': 35, 'category': 'GC', 'price': 7.0},
    'Filippo Ganna': {'team': 'INEOS Grenadiers', 'nationality': 'Italy', 'ranking': 25, 'category': 'ROULEUR', 'price': 7.5},
    'Santiago Buitrago': {'team': 'Bahrain Victorious', 'nationality': 'Colombia', 'ranking': 49, 'category': 'CLIMBER', 'price': 6.0},
    'Antonio Tiberi': {'team': 'Bahrain Victorious', 'nationality': 'Italy', 'ranking': 48, 'category': 'GC', 'price': 6.0},
    'Matej Mohorič': {'team': 'Bahrain Victorious', 'nationality': 'Slovenia', 'ranking': 51, 'category': 'CLASSICS', 'price': 5.5},
    'Pello Bilbao': {'team': 'Bahrain Victorious', 'nationality': 'Spain', 'ranking': 32, 'category': 'CLIMBER', 'price': 7.0},
    'David Gaudu': {'team': 'Groupama-FDJ United', 'nationality': 'France', 'ranking': 27, 'category': 'CLIMBER', 'price': 7.5},
    'Romain Grégoire': {'team': 'Groupama-FDJ United', 'nationality': 'France', 'ranking': 24, 'category': 'ROULEUR', 'price': 7.5},
    'Lenny Martinez': {'team': 'Groupama-FDJ United', 'nationality': 'France', 'ranking': 26, 'category': 'CLIMBER', 'price': 7.5},
    'Giulio Ciccone': {'team': 'Lidl-Trek', 'nationality': 'Italy', 'ranking': 37, 'category': 'CLIMBER', 'price': 6.5},
    'Jai Hindley': {'team': 'Red Bull-BORA-hansgrohe', 'nationality': 'Australia', 'ranking': 44, 'category': 'GC', 'price': 6.5},
    'Aleksandr Vlasov': {'team': 'Red Bull-BORA-hansgrohe', 'nationality': 'Russia', 'ranking': 43, 'category': 'GC', 'price': 6.5},
    'Jonas Abrahamsen': {'team': 'Uno-X Mobility', 'nationality': 'Norway', 'ranking': 45, 'category': 'ROULEUR', 'price': 6.0},
    'Magnus Cort': {'team': 'Uno-X Mobility', 'nationality': 'Denmark', 'ranking': 47, 'category': 'SPRINTER', 'price': 6.0},
    'Tobias Halland Johannessen': {'team': 'Uno-X Mobility', 'nationality': 'Norway', 'ranking': 46, 'category': 'CLIMBER', 'price': 6.0},
    'Simon Yates': {'team': 'Team Jayco-AlUla', 'nationality': 'United Kingdom', 'ranking': 39, 'category': 'GC', 'price': 6.5},
    'Michael Matthews': {'team': 'Team Jayco-AlUla', 'nationality': 'Australia', 'ranking': 41, 'category': 'SPRINTER', 'price': 6.0},
    'Max Poole': {'team': 'Team Picnic-PostNL', 'nationality': 'United Kingdom', 'ranking': 55, 'category': 'CLIMBER', 'price': 5.5},
    'Nairo Quintana': {'team': 'Movistar Team', 'nationality': 'Colombia', 'ranking': 50, 'category': 'CLIMBER', 'price': 6.0},
    'Neilson Powless': {'team': 'EF Education-EasyPost', 'nationality': 'USA', 'ranking': 28, 'category': 'CLIMBER', 'price': 7.5},
    'Jay Vine': {'team': 'UAE Team Emirates-XRG', 'nationality': 'Australia', 'ranking': 25, 'category': 'CLIMBER', 'price': 7.5},
    'Marc Hirschi': {'team': 'UAE Team Emirates-XRG', 'nationality': 'Switzerland', 'ranking': 14, 'category': 'CLIMBER', 'price': 9.0},
    'Olav Kooij': {'team': 'Team Visma-Lease a Bike', 'nationality': 'Netherlands', 'ranking': 18, 'category': 'SPRINTER', 'price': 8.5},
    'Arnaud De Lie': {'team': 'Lotto-Intermarché', 'nationality': 'Belgium', 'ranking': 18, 'category': 'SPRINTER', 'price': 8.5},
    'Julian Alaphilippe': {'team': 'Soudal Quick-Step', 'nationality': 'France', 'ranking': 34, 'category': 'CLASSICS', 'price': 7.0},
}

def is_likely_team_name(name):
    """Check if name looks like a team name"""
    name_lower = name.lower()
    return any(pattern in name_lower for pattern in TEAM_PATTERNS)

def main():
    # Read the raw CSV
    with open('wiki_cyclists.csv', 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f, fieldnames=['first_name', 'last_name', 'team', 'nationality', 'age', 'uci_ranking', 'speciality', 'price', 'category'])
        rows = list(reader)

    print(f'Read {len(rows)} rows from wiki_cyclists.csv')

    # Clean and enrich data
    cyclists = []
    seen_names = set()

    for row in rows:
        full_name = f"{row['first_name']} {row['last_name']}".strip()

        # Skip team names
        if is_likely_team_name(full_name):
            continue

        # Skip duplicates
        if full_name.lower() in seen_names:
            continue

        # Skip single word names
        if not row['last_name'] or len(row['last_name'].strip()) < 2:
            continue

        seen_names.add(full_name.lower())

        # Check if we have enriched data for this cyclist
        enriched = None
        for known_name, data in TOP_CYCLISTS.items():
            if known_name.lower() in full_name.lower() or full_name.lower() in known_name.lower():
                enriched = data
                break

        if enriched:
            cyclists.append({
                'first_name': row['first_name'],
                'last_name': row['last_name'],
                'team': enriched['team'],
                'nationality': enriched['nationality'],
                'age': '',
                'uci_ranking': str(enriched['ranking']),
                'speciality': enriched['category'],
                'price': str(enriched['price']),
                'category': enriched['category']
            })
        else:
            cyclists.append({
                'first_name': row['first_name'],
                'last_name': row['last_name'],
                'team': '',
                'nationality': '',
                'age': '',
                'uci_ranking': '',
                'speciality': '',
                'price': '5.0',
                'category': 'ROULEUR'
            })

    print(f'Cleaned to {len(cyclists)} cyclists')

    # Save clean CSV
    with open('worldtour_2026_complete.csv', 'w', newline='', encoding='utf-8') as f:
        fieldnames = ['first_name', 'last_name', 'team', 'nationality', 'age', 'uci_ranking', 'speciality', 'price', 'category']
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        for cyclist in cyclists:
            writer.writerow(cyclist)

    print(f'Saved to worldtour_2026_complete.csv')

    # Count enriched vs basic
    enriched_count = sum(1 for c in cyclists if c['team'])
    print(f'Enriched cyclists: {enriched_count}')
    print(f'Basic cyclists: {len(cyclists) - enriched_count}')

if __name__ == '__main__':
    main()
