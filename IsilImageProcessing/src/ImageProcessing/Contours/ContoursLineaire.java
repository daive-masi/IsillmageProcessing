package ImageProcessing.Contours;

import ImageProcessing.Lineaire.FiltrageLineaireLocal;

/**
 * Contient des méthodes statiques pour la détection de contours linéaire
 * basée sur des opérateurs de gradient (Prewitt, Sobel) et Laplacien,
 * implémentés via convolution.
 */
public class ContoursLineaire {

    // --- Masques de Prewitt (3x3) ---
    private static final double[][] PREWITT_H = {
            {-1, 0, 1},
            {-1, 0, 1},
            {-1, 0, 1}
    }; // Détecte les contours verticaux (gradient horizontal)

    private static final double[][] PREWITT_V = {
            {-1, -1, -1},
            { 0,  0,  0},
            { 1,  1,  1}
    }; // Détecte les contours horizontaux (gradient vertical)

    // --- Masques de Sobel (3x3) ---
    private static final double[][] SOBEL_H = {
            {-1, 0, 1},
            {-2, 0, 2},
            {-1, 0, 1}
    }; // Détecte les contours verticaux (gradient horizontal)

    private static final double[][] SOBEL_V = {
            {-1, -2, -1},
            { 0,  0,  0},
            { 1,  2,  1}
    }; // Détecte les contours horizontaux (gradient vertical)

    // --- Masques Laplaciens (3x3) ---
    private static final double[][] LAPLACIEN_4 = {
            { 0,  1,  0},
            { 1, -4,  1},
            { 0,  1,  0}
    }; // Formule 1.52

    private static final double[][] LAPLACIEN_8 = {
            { 1,  1,  1},
            { 1, -8,  1},
            { 1,  1,  1}
    }; // Formule 1.53


    /**
     * Calcule le gradient de Prewitt de l'image dans une direction spécifiée.
     *
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @param dir La direction du gradient : 1 pour horizontal (détection contours verticaux),
     *            2 pour vertical (détection contours horizontaux).
     * @return Une image (int[hauteur][largeur]) représentant le gradient.
     *         Note: Le résultat est clampé [0, 255] par la convolution sous-jacente.
     *         Les valeurs négatives du gradient sont mises à 0.
     *         Retourne null si l'entrée est invalide.
     *
     * @technique Opérateur de Prewitt, Convolution 2D.
     */
    public static int[][] gradientPrewitt(int[][] image, int dir) {
        if (image == null) return null;
        switch (dir) {
            case 1: // Horizontal
                System.out.println("Calcul Gradient Prewitt Horizontal...");
                return FiltrageLineaireLocal.filtreMasqueConvolution(image, PREWITT_H);
            case 2: // Vertical
                System.out.println("Calcul Gradient Prewitt Vertical...");
                return FiltrageLineaireLocal.filtreMasqueConvolution(image, PREWITT_V);
            default:
                System.err.println("Erreur [gradientPrewitt]: Direction invalide (1 ou 2).");
                return null;
        }
    }

    /**
     * Calcule le gradient de Sobel de l'image dans une direction spécifiée.
     *
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @param dir La direction du gradient : 1 pour horizontal (détection contours verticaux),
     *            2 pour vertical (détection contours horizontaux).
     * @return Une image (int[hauteur][largeur]) représentant le gradient.
     *         Note: Le résultat est clampé [0, 255] par la convolution sous-jacente.
     *         Retourne null si l'entrée est invalide.
     *
     * @technique Opérateur de Sobel, Convolution 2D.
     */
    public static int[][] gradientSobel(int[][] image, int dir) {
        if (image == null) return null;
        switch (dir) {
            case 1: // Horizontal
                System.out.println("Calcul Gradient Sobel Horizontal...");
                return FiltrageLineaireLocal.filtreMasqueConvolution(image, SOBEL_H);
            case 2: // Vertical
                System.out.println("Calcul Gradient Sobel Vertical...");
                return FiltrageLineaireLocal.filtreMasqueConvolution(image, SOBEL_V);
            default:
                System.err.println("Erreur [gradientSobel]: Direction invalide (1 ou 2).");
                return null;
        }
    }

    /**
     * Calcule le Laplacien 4-connexe de l'image.
     * Utile pour la détection de contours (passages par zéro) ou le rehaussement.
     *
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @return Une image (int[hauteur][largeur]) représentant le Laplacien 4.
     *         Note: Le résultat est clampé [0, 255] par la convolution sous-jacente.
     *         Retourne null si l'entrée est invalide.
     *
     * @technique Opérateur Laplacien (4-connexité), Convolution 2D. Formule 1.52.
     */
    public static int[][] laplacien4(int[][] image) {
        if (image == null) return null;
        System.out.println("Calcul Laplacien 4...");
        return FiltrageLineaireLocal.filtreMasqueConvolution(image, LAPLACIEN_4);
    }

    /**
     * Calcule le Laplacien 8-connexe de l'image.
     * Utile pour la détection de contours (passages par zéro) ou le rehaussement.
     *
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @return Une image (int[hauteur][largeur]) représentant le Laplacien 8.
     *         Note: Le résultat est clampé [0, 255] par la convolution sous-jacente.
     *         Retourne null si l'entrée est invalide.
     *
     * @technique Opérateur Laplacien (8-connexité), Convolution 2D. Formule 1.53.
     */
    public static int[][] laplacien8(int[][] image) {
        if (image == null) return null;
        System.out.println("Calcul Laplacien 8...");
        return FiltrageLineaireLocal.filtreMasqueConvolution(image, LAPLACIEN_8);
    }

}