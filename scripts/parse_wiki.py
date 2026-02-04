#!/usr/bin/env python3
"""
Parse Wikipedia UCI WorldTeams data and extract cyclists
"""

import json
import re
import csv

def main():
    # Load the JSON
    with open('wiki_uci.json', 'r', encoding='utf-8') as f:
        data = json.load(f)

    html = data['parse']['text']['*']
    print(f'HTML length: {len(html)}')

    # Find all table rows with rider data
    # Pattern: row with flag + name link

    # First, let's find cyclist names that appear as links
    # Cyclist links typically have format: title="Name Name" or title="Name Name (cyclist)"

    pattern = r'<a[^>]*href="/wiki/([^"]+)"[^>]*title="([^"]+)"[^>]*>([^<]+)</a>'
    matches = re.findall(pattern, html)

    cyclists = []
    seen = set()

    # Countries to filter out
    countries = {'belgium', 'france', 'spain', 'germany', 'italy', 'united states', 'australia',
                 'netherlands', 'switzerland', 'denmark', 'norway', 'portugal', 'slovenia',
                 'colombia', 'ecuador', 'ireland', 'eritrea', 'great britain', 'united kingdom',
                 'austria', 'poland', 'canada', 'south africa', 'new zealand', 'kazakhstan',
                 'russia', 'ukraine', 'czech republic', 'slovakia', 'latvia', 'estonia', 'lithuania',
                 'luxembourg', 'bahrain', 'asia', 'europe', 'oceania', 'north america', 'africa'}

    # Terms to filter out
    skip_terms = {'team', 'cycling', 'tour', 'race', 'uci', 'world', 'edit', 'wiki',
                  'stage', 'grand', 'classification', 'jersey', 'champion', 'olympic',
                  'continental', 'pro team', 'worldteam'}

    for href, title, text in matches:
        # Clean text
        name = text.strip()
        title_lower = title.lower()

        # Skip if already seen
        if name.lower() in seen:
            continue

        # Skip countries and non-cyclist terms
        if name.lower() in countries:
            continue
        if any(term in title_lower for term in skip_terms):
            continue
        if any(term in name.lower() for term in skip_terms):
            continue

        # Must have at least 2 words (first + last name)
        words = name.split()
        if len(words) < 2:
            continue

        # Skip if contains numbers or special chars (except common name chars)
        if any(c.isdigit() for c in name):
            continue

        # Likely a cyclist name
        if '(cyclist)' in title_lower or (len(words) == 2 and all(w[0].isupper() for w in words if w)):
            seen.add(name.lower())

            # Try to extract nationality from nearby context
            # For now, just add the name
            first_name = words[0]
            last_name = ' '.join(words[1:])

            cyclists.append({
                'first_name': first_name,
                'last_name': last_name,
                'team': '',  # Would need more parsing
                'nationality': '',
                'age': '',
                'uci_ranking': '',
                'speciality': '',
                'price': '5.0',
                'category': 'ROULEUR'
            })
            try:
                print(f'Found: {name}')
            except UnicodeEncodeError:
                print(f'Found: {name.encode("ascii", "replace").decode()}')

    print(f'\nTotal cyclists found: {len(cyclists)}')

    # Save to CSV
    if cyclists:
        with open('wiki_cyclists.csv', 'w', newline='', encoding='utf-8') as f:
            fieldnames = ['first_name', 'last_name', 'team', 'nationality', 'age',
                          'uci_ranking', 'speciality', 'price', 'category']
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            for cyclist in cyclists:
                writer.writerow(cyclist)
        print(f'Saved to wiki_cyclists.csv')

if __name__ == '__main__':
    main()
