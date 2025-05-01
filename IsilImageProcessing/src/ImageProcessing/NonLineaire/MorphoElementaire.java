package ImageProcessing.NonLineaire;

import ImageProcessing.Core.PaddingUtils;
import ImageProcessing.Core.BorderMode;

/**
 * Contient des méthodes statiques pour les opérations morphologiques élémentaires
 * (érosion, dilatation, ouverture, fermeture) sur des images en niveaux de gris ou binaires.
 * Utilise un élément structurant carré implicite de taille spécifiée.
 */
public class MorphoElementaire {

    /**
     * Réalise l'érosion morphologique d'une image.
     * Pour chaque pixel, la valeur de sortie est le minimum des pixels
     * dans le voisinage défini par l'élément structurant carré.
     * Gère les bords par réplication.
     *
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @param tailleMasque La taille (largeur/hauteur) de l'élément structurant carré (doit être impair >= 1).
     * @return Une nouvelle image (int[hauteur][largeur]) résultat de l'érosion.
     *         Retourne null si les entrées sont invalides.
     *
     * @technique Morphologie Mathématique, Érosion, Élément Structurant Carré.
     */
    public static int[][] erosion(int[][] image, int tailleMasque) {
        // --- Validation des entrées ---
        if (image == null || image.length == 0 || image[0].length == 0) {
            System.err.println("Erreur [erosion]: L'image d'entrée est invalide.");
            return null;
        }
        if (tailleMasque <= 0) {
            System.err.println("Erreur [erosion]: La taille du masque doit être positive.");
            return null;
        }
        if (tailleMasque % 2 == 0) {
            System.err.println("Erreur [erosion]: La taille du masque doit être impaire.");
            return null;
        }

        int hauteur = image.length;
        int largeur = image[0].length;
        int padSize = (tailleMasque - 1) / 2;

        // --- Padding ---
        // REPLICATE est un choix raisonnable pour l'érosion.
        int[][] imagePaddee = PaddingUtils.padImage(image, padSize, BorderMode.REPLICATE);
        if (imagePaddee == null) {
            System.err.println("Erreur [erosion]: Échec lors du padding de l'image.");
            return null;
        }

        // Création de l'image résultat
        int[][] resultat = new int[hauteur][largeur];

        // --- Application de l'érosion ---
        // Itérer sur chaque pixel (y, x) de l'image *originale*
        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                int minVoisin = 255; // Initialiser au maximum possible pour chercher le minimum

                // Itérer sur le voisinage carré dans l'image *paddée*
                for (int i = -padSize; i <= padSize; i++) {
                    for (int j = -padSize; j <= padSize; j++) {
                        int voisinY = y + padSize + i;
                        int voisinX = x + padSize + j;
                        minVoisin = Math.min(minVoisin, imagePaddee[voisinY][voisinX]);
                    }
                }
                // Stocker le minimum trouvé
                resultat[y][x] = minVoisin;
            }
        }
        return resultat;
    }

    /**
     * Réalise la dilatation morphologique d'une image.
     * Pour chaque pixel, la valeur de sortie est le maximum des pixels
     * dans le voisinage défini par l'élément structurant carré.
     * Gère les bords par réplication.
     *
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @param tailleMasque La taille (largeur/hauteur) de l'élément structurant carré (doit être impair >= 1).
     * @return Une nouvelle image (int[hauteur][largeur]) résultat de la dilatation.
     *         Retourne null si les entrées sont invalides.
     *
     * @technique Morphologie Mathématique, Dilatation, Élément Structurant Carré.
     */
    public static int[][] dilatation(int[][] image, int tailleMasque) {
        // --- Validation des entrées ---
        if (image == null || image.length == 0 || image[0].length == 0) {
            System.err.println("Erreur [dilatation]: L'image d'entrée est invalide.");
            return null;
        }
        if (tailleMasque <= 0) {
            System.err.println("Erreur [dilatation]: La taille du masque doit être positive.");
            return null;
        }
        if (tailleMasque % 2 == 0) {
            System.err.println("Erreur [dilatation]: La taille du masque doit être impaire.");
            return null;
        }

        int hauteur = image.length;
        int largeur = image[0].length;
        int padSize = (tailleMasque - 1) / 2;

        // --- Padding ---
        // REPLICATE est utilisé ici aussi pour la cohérence avec l'érosion.
        // Pourrait être discuté (parfois ZERO est utilisé pour la dilatation).
        int[][] imagePaddee = PaddingUtils.padImage(image, padSize, BorderMode.REPLICATE);
        if (imagePaddee == null) {
            System.err.println("Erreur [dilatation]: Échec lors du padding de l'image.");
            return null;
        }

        // Création de l'image résultat
        int[][] resultat = new int[hauteur][largeur];

        // --- Application de la dilatation ---
        // Itérer sur chaque pixel (y, x) de l'image *originale*
        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                int maxVoisin = 0; // Initialiser au minimum possible pour chercher le maximum

                // Itérer sur le voisinage carré dans l'image *paddée*
                for (int i = -padSize; i <= padSize; i++) {
                    for (int j = -padSize; j <= padSize; j++) {
                        int voisinY = y + padSize + i;
                        int voisinX = x + padSize + j;
                        maxVoisin = Math.max(maxVoisin, imagePaddee[voisinY][voisinX]);
                    }
                }
                // Stocker le maximum trouvé
                resultat[y][x] = maxVoisin;
            }
        }
        return resultat;
    }

    /**
     * Réalise l'ouverture morphologique d'une image.
     * L'ouverture est une érosion suivie d'une dilatation avec le même élément structurant.
     * Elle tend à supprimer les petits objets brillants et à lisser les contours internes.
     *
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @param tailleMasque La taille de l'élément structurant carré (doit être impair >= 1).
     * @return Une nouvelle image (int[hauteur][largeur]) résultat de l'ouverture.
     *         Retourne null si les entrées sont invalides ou si une étape échoue.
     *
     * @technique Morphologie Mathématique, Ouverture.
     */
    public static int[][] ouverture(int[][] image, int tailleMasque) {
        // Valider juste la taille ici, les fonctions internes valideront l'image
        if (tailleMasque <= 0 || tailleMasque % 2 == 0) {
            System.err.println("Erreur [ouverture]: Taille du masque invalide.");
            return null;
        }

        System.out.println("Ouverture: Étape 1 - Érosion...");
        int[][] imageErodee = erosion(image, tailleMasque);
        if (imageErodee == null) {
            System.err.println("Erreur [ouverture]: Échec de l'étape d'érosion.");
            return null;
        }

        System.out.println("Ouverture: Étape 2 - Dilatation...");
        int[][] resultat = dilatation(imageErodee, tailleMasque);
        if (resultat == null) {
            System.err.println("Erreur [ouverture]: Échec de l'étape de dilatation.");
            return null;
        }

        System.out.println("Ouverture terminée.");
        return resultat;
    }

    /**
     * Réalise la fermeture morphologique d'une image.
     * La fermeture est une dilatation suivie d'une érosion avec le même élément structurant.
     * Elle tend à combler les petits trous sombres et à lisser les contours externes.
     *
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @param tailleMasque La taille de l'élément structurant carré (doit être impair >= 1).
     * @return Une nouvelle image (int[hauteur][largeur]) résultat de la fermeture.
     *         Retourne null si les entrées sont invalides ou si une étape échoue.
     *
     * @technique Morphologie Mathématique, Fermeture.
     */
    public static int[][] fermeture(int[][] image, int tailleMasque) {
        // Valider juste la taille ici, les fonctions internes valideront l'image
        if (tailleMasque <= 0 || tailleMasque % 2 == 0) {
            System.err.println("Erreur [fermeture]: Taille du masque invalide.");
            return null;
        }

        System.out.println("Fermeture: Étape 1 - Dilatation...");
        int[][] imageDilatee = dilatation(image, tailleMasque);
        if (imageDilatee == null) {
            System.err.println("Erreur [fermeture]: Échec de l'étape de dilatation.");
            return null;
        }

        System.out.println("Fermeture: Étape 2 - Érosion...");
        int[][] resultat = erosion(imageDilatee, tailleMasque);
        if (resultat == null) {
            System.err.println("Erreur [fermeture]: Échec de l'étape d'érosion.");
            return null;
        }

        System.out.println("Fermeture terminée.");
        return resultat;
    }
}