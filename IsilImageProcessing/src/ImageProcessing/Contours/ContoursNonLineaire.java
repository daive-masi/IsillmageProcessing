package ImageProcessing.Contours;

import ImageProcessing.NonLineaire.MorphoElementaire; // Assurez-vous d'avoir cette classe

/**
 * Contient des méthodes statiques pour la détection de contours non-linéaire
 * basée sur des opérateurs morphologiques (érosion, dilatation).
 */
public class ContoursNonLineaire {

    // Taille implicite de l'élément structurant carré pour les opérations morphologiques.
    // 3x3 est une valeur courante et généralement suffisante pour ces opérateurs.
    private static final int DEFAULT_SE_SIZE = 3;

    /**
     * Calcule le gradient morphologique basé sur l'érosion (Contour Intérieur).
     * Formule: G_erosion(f) = f - erosion(f)
     * Met en évidence les pixels qui disparaissent lors d'une érosion.
     *
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @return Une image (int[hauteur][largeur]) représentant le gradient d'érosion.
     *         Retourne null si l'entrée est invalide.
     *
     * @technique Gradient Morphologique, Érosion.
     */
    public static int[][] gradientErosion(int[][] image) {
        if (image == null || image.length == 0 || image[0].length == 0) return null;
        System.out.println("Calcul Gradient Morphologique (Érosion)...");

        // 1. Calculer l'érosion de l'image
        int[][] erodedImage = MorphoElementaire.erosion(image, DEFAULT_SE_SIZE);
        if (erodedImage == null) {
            System.err.println("Erreur [gradientErosion]: Échec de l'érosion.");
            return null;
        }

        // 2. Calculer la différence: image - erosion(image)
        int hauteur = image.length;
        int largeur = image[0].length;
        int[][] gradient = new int[hauteur][largeur];

        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                // Soustraction et clamping implicite (si pixel < erodedPixel, résultat < 0 -> clampé à 0)
                int diff = image[y][x] - erodedImage[y][x];
                gradient[y][x] = Math.max(0, diff); // Assurer que le résultat est >= 0
                // Note: Pas besoin de clamper à 255 car diff <= pixel <= 255
            }
        }

        return gradient;
    }

    /**
     * Calcule le gradient morphologique basé sur la dilatation (Contour Extérieur).
     * Formule: G_dilatation(f) = dilatation(f) - f
     * Met en évidence les pixels qui sont ajoutés lors d'une dilatation.
     *
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @return Une image (int[hauteur][largeur]) représentant le gradient de dilatation.
     *         Retourne null si l'entrée est invalide.
     *
     * @technique Gradient Morphologique, Dilatation.
     */
    public static int[][] gradientDilatation(int[][] image) {
        if (image == null || image.length == 0 || image[0].length == 0) return null;
        System.out.println("Calcul Gradient Morphologique (Dilatation)...");

        // 1. Calculer la dilatation de l'image
        int[][] dilatedImage = MorphoElementaire.dilatation(image, DEFAULT_SE_SIZE);
        if (dilatedImage == null) {
            System.err.println("Erreur [gradientDilatation]: Échec de la dilatation.");
            return null;
        }

        // 2. Calculer la différence: dilatation(image) - image
        int hauteur = image.length;
        int largeur = image[0].length;
        int[][] gradient = new int[hauteur][largeur];

        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                // Soustraction et clamping implicite
                int diff = dilatedImage[y][x] - image[y][x];
                gradient[y][x] = Math.max(0, diff); // Assurer >= 0
                // Clamper aussi à 255 ? Théoriquement pas nécessaire si dilatedImage est bien calculé
                // gradient[y][x] = Math.max(0, Math.min(255, diff));
            }
        }

        return gradient;
    }

    /**
     * Calcule le gradient morphologique de Beucher (Symétrique).
     * Formule: G_beucher(f) = dilatation(f) - erosion(f)
     * Donne généralement des contours plus épais et centrés.
     *
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @return Une image (int[hauteur][largeur]) représentant le gradient de Beucher.
     *         Retourne null si l'entrée est invalide.
     *
     * @technique Gradient Morphologique de Beucher, Dilatation, Érosion.
     */
    public static int[][] gradientBeucher(int[][] image) {
        if (image == null || image.length == 0 || image[0].length == 0) return null;
        System.out.println("Calcul Gradient Morphologique (Beucher)...");

        // 1. Calculer l'érosion
        int[][] erodedImage = MorphoElementaire.erosion(image, DEFAULT_SE_SIZE);
        if (erodedImage == null) {
            System.err.println("Erreur [gradientBeucher]: Échec de l'érosion.");
            return null;
        }

        // 2. Calculer la dilatation
        int[][] dilatedImage = MorphoElementaire.dilatation(image, DEFAULT_SE_SIZE);
        if (dilatedImage == null) {
            System.err.println("Erreur [gradientBeucher]: Échec de la dilatation.");
            return null;
        }

        // 3. Calculer la différence: dilatation(image) - erosion(image)
        int hauteur = image.length;
        int largeur = image[0].length;
        int[][] gradient = new int[hauteur][largeur];

        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                // Soustraction et clamping implicite
                int diff = dilatedImage[y][x] - erodedImage[y][x];
                gradient[y][x] = Math.max(0, diff); // Assurer >= 0
                // gradient[y][x] = Math.max(0, Math.min(255, diff)); // Clamper à 255 ?
            }
        }

        return gradient;
    }

    /**
     * Calcule le Laplacien morphologique non-linéaire.
     * Formule: Lap(f) = (dilatation(f) + erosion(f) - 2*f) / 2
     * Donne une mesure de la courbure locale. Le résultat peut être négatif.
     *
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @return Une image (int[hauteur][largeur]) représentant le Laplacien non-linéaire.
     *         Note: Le résultat est clampé [0, 255] pour l'affichage.
     *         Retourne null si l'entrée est invalide.
     *
     * @technique Laplacien Morphologique. Formule 1.60 (adaptée avec division par 2).
     */
    public static int[][] laplacienNonLineaire(int[][] image) {
        if (image == null || image.length == 0 || image[0].length == 0) return null;
        System.out.println("Calcul Laplacien Morphologique Non-Linéaire...");

        // 1. Calculer l'érosion
        int[][] erodedImage = MorphoElementaire.erosion(image, DEFAULT_SE_SIZE);
        if (erodedImage == null) {
            System.err.println("Erreur [laplacienNonLineaire]: Échec de l'érosion.");
            return null;
        }

        // 2. Calculer la dilatation
        int[][] dilatedImage = MorphoElementaire.dilatation(image, DEFAULT_SE_SIZE);
        if (dilatedImage == null) {
            System.err.println("Erreur [laplacienNonLineaire]: Échec de la dilatation.");
            return null;
        }

        // 3. Calculer le Laplacien: (dilatation + erosion - 2*image) / 2
        int hauteur = image.length;
        int largeur = image[0].length;
        int[][] laplacien = new int[hauteur][largeur];

        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                // Calculer la somme/différence en double pour précision intermédiaire
                double lapVal = ( (double)dilatedImage[y][x] + erodedImage[y][x] - 2.0 * image[y][x] ) / 2.0;

                // Arrondir et Clamper à [0, 255] pour l'affichage
                int lapInt = (int) Math.round(lapVal);
                laplacien[y][x] = Math.max(0, Math.min(255, lapInt));
            }
        }

        return laplacien;
    }
}