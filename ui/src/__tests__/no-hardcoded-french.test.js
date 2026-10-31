/**
 * Guard: every text shown to the user comes from the i18n catalogs. No French may stay hardcoded in a component, a
 * view or a dataset, nor a French number format.
 */
import { describe, expect, it } from 'vitest'
import en from '../i18n/en.js'
import fr from '../i18n/fr.js'

const sources = import.meta.glob(['../**/*.{vue,js}', '!../__tests__/**', '!../i18n/**'], { query: '?raw', import: 'default', eager: true })

const FRENCH = /[éèêàçùôîïœ][a-zA-Z]|\b(Accueil|Projets|Outils|Souscriptions|Utilisateurs|Rechercher|Annuler|Enregistrer|Supprimer|Fermer|Ajouter|Modifier|Groupes|Erreur|Chargement|Aucune?|Profil|Saisissez|Sélectionner)\b|fr-FR/

/** Remove the comments: they are not displayed. */
function code(source) {
  return source
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .split('\n')
    .filter((line) => !line.trim().startsWith('//'))
    .map((line) => line.replace(/\s\/\/\s.*$/, ''))
    .join('\n')
}

describe('hardcoded French', () => {
  it('has the search texts in both languages', () => {
    for (const key of ['company.scopePlaceholder', 'user.companyPlaceholder', 'user.groupPlaceholder', 'id.search.noScope', 'id.search.noEntity', 'id.search.noGroup', 'id.search.typeToSearch']) {
      expect(en[key]).toBeTruthy()
      expect(fr[key]).toBeTruthy()
    }
  })

  it('finds source files', () => {
    expect(Object.keys(sources).length).toBeGreaterThan(10)
  })

  it('has none outside the i18n catalogs', () => {
    const found = []
    for (const [path, source] of Object.entries(sources)) {
      code(source).split('\n').forEach((line) => {
        if (FRENCH.test(line.replace(/Français/g, ''))) found.push(`${path}: ${line.trim().slice(0, 90)}`)
      })
    }
    expect(found).toEqual([])
  })
})
