package ImageProcessing.Core;

/**
 * Définit les différentes stratégies de gestion des bords pour le padding.
 */
public enum BorderMode {
    /** Remplit les bords avec des zéros. */
    ZERO,
    /** Répète la valeur du pixel le plus proche sur le bord. */
    REPLICATE,
    /** Utilise une réflexion en miroir des pixels près du bord. */
    MIRROR
    // On pourrait ajouter d'autres modes comme WRAP (circulaire) si nécessaire.
}