package ImageProcessing.Lineaire;

import ImageProcessing.Complexe.Complexe;
import ImageProcessing.Complexe.MatriceComplexe;
import ImageProcessing.Fourier.Fourier; // Utiliser la classe Fourier fournie

/**
 * Contient des méthodes statiques pour le filtrage linéaire global (fréquentiel) d'images.
 * Ces méthodes opèrent dans le domaine de Fourier.
 */
public class FiltrageLinaireGlobal {

    // Facteur de normalisation potentiel pour l'IFFT (à ajuster si besoin après tests)
    // Si la FFT divise par N*M et l'IFFT ne multiplie pas, on doit multiplier ici.
    private static final boolean APPLY_IFFT_NORMALIZATION = true;

    /**
     * Filtre passe-bas idéal dans le domaine fréquentiel.
     * Atténue (met à zéro) les hautes fréquences au-delà d'une fréquence de coupure.
     *
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @param frequenceCoupure Le rayon (en "pixels fréquentiels") du cercle définissant
     *                         la coupure dans le spectre centré.
     * @return L'image filtrée (int[hauteur][largeur]). Retourne null si l'entrée est invalide.
     *
     * @technique Filtrage Fréquentiel, Filtre Passe-Bas Idéal, Transformée de Fourier 2D.
     */
    public static int[][] filtrePasseBasIdeal(int[][] image, int frequenceCoupure) {
        return appliquerFiltreFrequentiel(image, (rows, cols, D0) ->
                        creerFiltreIdeal(rows, cols, D0, true), // true pour Passe-Bas
                frequenceCoupure
        );
    }

    /**
     * Filtre passe-haut idéal dans le domaine fréquentiel.
     * Atténue (met à zéro) les basses fréquences en deçà d'une fréquence de coupure.
     *
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @param frequenceCoupure Le rayon (en "pixels fréquentiels") du cercle définissant
     *                         la coupure dans le spectre centré.
     * @return L'image filtrée (int[hauteur][largeur]). Retourne null si l'entrée est invalide.
     *
     * @technique Filtrage Fréquentiel, Filtre Passe-Haut Idéal, Transformée de Fourier 2D.
     */
    public static int[][] filtrePasseHautIdeal(int[][] image, int frequenceCoupure) {
        return appliquerFiltreFrequentiel(image, (rows, cols, D0) ->
                        creerFiltreIdeal(rows, cols, D0, false), // false pour Passe-Haut
                frequenceCoupure
        );
    }

    /**
     * Filtre passe-bas de Butterworth dans le domaine fréquentiel.
     * Atténue progressivement les hautes fréquences au-delà d'une fréquence de coupure.
     *
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @param frequenceCoupure La fréquence où le gain du filtre est réduit (typiquement à 1/sqrt(2)).
     * @param ordre L'ordre du filtre (n). Plus l'ordre est élevé, plus la coupure est nette. Typiquement >= 1.
     * @return L'image filtrée (int[hauteur][largeur]). Retourne null si l'entrée est invalide.
     *
     * @technique Filtrage Fréquentiel, Filtre Passe-Bas Butterworth, Transformée de Fourier 2D.
     */
    public static int[][] filtrePasseBasButterworth(int[][] image, int frequenceCoupure, int ordre) {
        return appliquerFiltreFrequentiel(image, (rows, cols, D0) ->
                        creerFiltreButterworth(rows, cols, D0, ordre, true), // true pour Passe-Bas
                frequenceCoupure
        );
    }

    /**
     * Filtre passe-haut de Butterworth dans le domaine fréquentiel.
     * Atténue progressivement les basses fréquences en deçà d'une fréquence de coupure.
     *
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @param frequenceCoupure La fréquence où le gain du filtre est réduit.
     * @param ordre L'ordre du filtre (n). Plus l'ordre est élevé, plus la coupure est nette. Typiquement >= 1.
     * @return L'image filtrée (int[hauteur][largeur]). Retourne null si l'entrée est invalide.
     *
     * @technique Filtrage Fréquentiel, Filtre Passe-Haut Butterworth, Transformée de Fourier 2D.
     */
    public static int[][] filtrePasseHautButterworth(int[][] image, int frequenceCoupure, int ordre) {
        return appliquerFiltreFrequentiel(image, (rows, cols, D0) ->
                        creerFiltreButterworth(rows, cols, D0, ordre, false), // false pour Passe-Haut
                frequenceCoupure
        );
    }


    // --- Méthode Générique pour Appliquer un Filtre Fréquentiel ---

    // Interface fonctionnelle pour représenter la logique de création de filtre
    @FunctionalInterface
    private interface FiltreFactory {
        MatriceComplexe create(int rows, int cols, double frequenceCoupure);
    }

    /**
     * Méthode centrale qui applique la chaîne de traitement du filtrage fréquentiel.
     *
     * @param image L'image d'entrée int[y][x].
     * @param factory L'objet capable de créer le masque de filtrage H(u,v).
     * @param frequenceCoupure La fréquence de coupure D0 pour le filtre.
     * @return L'image filtrée int[y][x].
     */
    private static int[][] appliquerFiltreFrequentiel(int[][] image, FiltreFactory factory, double frequenceCoupure) {
        if (image == null || image.length == 0 || image[0].length == 0) {
            System.err.println("Erreur [appliquerFiltreFrequentiel]: Image d'entrée invalide.");
            return null;
        }
        if (frequenceCoupure <= 0) {
            System.err.println("Erreur [appliquerFiltreFrequentiel]: Fréquence de coupure doit être positive.");
            return null;
        }

        int hauteur = image.length;
        int largeur = image[0].length;

        // 1. Convertir l'image int[][] en double[][] pour Fourier.Fourier2D
        double[][] imageDouble = new double[hauteur][largeur];
        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                imageDouble[y][x] = (double) image[y][x];
            }
        }

        // 2. Calculer la FFT
        System.out.println("Calcul FFT..."); // Log
        MatriceComplexe spectre = Fourier.Fourier2D(imageDouble);
        if (spectre == null) {
            System.err.println("Erreur [appliquerFiltreFrequentiel]: Échec du calcul FFT.");
            return null;
        }

        // 3. Centrer le spectre
        System.out.println("Centrage Spectre..."); // Log
        MatriceComplexe spectreCentre = Fourier.decroise(spectre);

        // 4. Créer le filtre H(u,v)
        System.out.println("Création Filtre H(u,v)..."); // Log
        MatriceComplexe filtreH = factory.create(hauteur, largeur, frequenceCoupure);
        if (filtreH == null) {
            System.err.println("Erreur [appliquerFiltreFrequentiel]: Échec de la création du filtre.");
            return null;
        }

        // 5. Appliquer le filtre (multiplication point par point)
        System.out.println("Application Filtre..."); // Log
        MatriceComplexe spectreFiltreCentre = new MatriceComplexe(hauteur, largeur);
        for (int u = 0; u < hauteur; u++) { // u correspond aux lignes (fréquences verticales)
            for (int v = 0; v < largeur; v++) { // v correspond aux colonnes (fréquences horizontales)
                Complexe valSpectre = spectreCentre.get(u, v);
                Complexe valFiltre = filtreH.get(u, v);

                // Copier valSpectre pour ne pas modifier l'original si multiplie modifie en place
                Complexe resMult = new Complexe(valSpectre.getPartieReelle(), valSpectre.getPartieImaginaire());
                resMult.multiplie(valFiltre); // Sf(u,v) = S(u,v) * H(u,v)

                spectreFiltreCentre.set(u, v, resMult);
            }
        }

        // 6. Dé-centrer le spectre filtré
        System.out.println("Dé-centrage Spectre..."); // Log
        MatriceComplexe spectreFiltre = Fourier.decroise(spectreFiltreCentre);

        // 7. Calculer l'IFFT
        System.out.println("Calcul IFFT..."); // Log
        MatriceComplexe imageComplexeFiltree = Fourier.InverseFourier2D(spectreFiltre);
        if (imageComplexeFiltree == null) {
            System.err.println("Erreur [appliquerFiltreFrequentiel]: Échec du calcul IFFT.");
            return null;
        }

// ... (après le calcul de imageComplexeFiltree = Fourier.InverseFourier2D(...))

// 8. Extraire partie réelle, trouver min/max, normaliser et convertir en int[0..255]
        System.out.println("Extraction, Recherche Min/Max et Normalisation Adaptative..."); // Log
        double[][] partieReelle = imageComplexeFiltree.getPartieReelle();
        int[][] imageResultat = new int[hauteur][largeur];

// --- Étape 8a: Trouver le Min et le Max de la partie réelle ---
        double minVal = Double.POSITIVE_INFINITY;
        double maxVal = Double.NEGATIVE_INFINITY;
        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                double val = partieReelle[y][x];
                if (val < minVal) {
                    minVal = val;
                }
                if (val > maxVal) {
                    maxVal = val;
                }
            }
        }
        System.out.println("Min/Max après IFFT: [" + minVal + ", " + maxVal + "]"); // Log

// --- Étape 8b: Appliquer la mise à l'échelle linéaire ---
        double range = maxVal - minVal;
// Gérer le cas où l'image est complètement uniforme (pour éviter division par zéro)
        if (range < 1e-6) {
            range = 1.0; // ou choisir une valeur par défaut, par exemple la moyenne
            System.out.println("Attention: Plage dynamique très faible après IFFT.");
        }

        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                double valReelle = partieReelle[y][x];

                // Mise à l'échelle: (val - min) / (max - min) pour ramener dans [0, 1]
                // Puis multiplier par 255 pour ramener dans [0, 255]
                double scaledValue = 255.0 * (valReelle - minVal) / range;

                // Arrondir et Clamper (le clamping est techniquement moins nécessaire après
                // une mise à l'échelle correcte, mais c'est une sécurité)
                int valInt = (int) Math.round(scaledValue);
                imageResultat[y][x] = Math.max(0, Math.min(255, valInt));
            }
        }

        System.out.println("Filtrage fréquentiel terminé."); // Log
        return imageResultat;
    }


    // --- Fonctions de Création des Filtres H(u,v) ---

    /** Crée un filtre idéal (passe-bas ou passe-haut). */
    private static MatriceComplexe creerFiltreIdeal(int rows, int cols, double D0, boolean isLowPass) {
        MatriceComplexe filtre = new MatriceComplexe(rows, cols);
        int centreY = rows / 2;
        int centreX = cols / 2;

        for (int u = 0; u < rows; u++) {
            for (int v = 0; v < cols; v++) {
                // Calculer la distance euclidienne par rapport au centre du spectre
                double distance = Math.sqrt(Math.pow(u - centreY, 2) + Math.pow(v - centreX, 2));

                // Appliquer la condition du filtre idéal
                boolean inPassBand;
                if (isLowPass) {
                    inPassBand = distance <= D0; // Passe si dans le cercle
                } else {
                    inPassBand = distance > D0; // Passe si hors du cercle
                }

                filtre.set(u, v, inPassBand ? 1.0 : 0.0, 0.0); // Filtre réel
            }
        }
        return filtre;
    }

    /** Crée un filtre de Butterworth (passe-bas ou passe-haut). */
    private static MatriceComplexe creerFiltreButterworth(int rows, int cols, double D0, int order, boolean isLowPass) {
        MatriceComplexe filtre = new MatriceComplexe(rows, cols);
        int centreY = rows / 2;
        int centreX = cols / 2;
        if (D0 == 0) D0 = 1e-6; // Eviter division par zéro si D0=0

        for (int u = 0; u < rows; u++) {
            for (int v = 0; v < cols; v++) {
                // Calculer la distance euclidienne par rapport au centre du spectre
                double distance = Math.sqrt(Math.pow(u - centreY, 2) + Math.pow(v - centreX, 2));

                // Calculer la valeur du filtre H(u,v) selon Butterworth
                double h_uv;
                if (isLowPass) {
                    // H(u,v) = 1 / (1 + (D(u,v)/D0)^(2n))
                    h_uv = 1.0 / (1.0 + Math.pow(distance / D0, 2.0 * order));
                } else { // Passe-Haut
                    // H(u,v) = 1 / (1 + (D0/D(u,v))^(2n))
                    if (distance == 0) distance = 1e-6; // Eviter division par zéro au centre
                    h_uv = 1.0 / (1.0 + Math.pow(D0 / distance, 2.0 * order));
                }

                filtre.set(u, v, h_uv, 0.0); // Filtre réel
            }
        }
        return filtre;
    }

}