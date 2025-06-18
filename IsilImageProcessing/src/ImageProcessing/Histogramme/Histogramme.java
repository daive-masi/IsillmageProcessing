package ImageProcessing.Histogramme;


/**
 * Contient des méthodes statiques pour le calcul et la manipulation
 * d'histogrammes d'images en niveaux de gris, ainsi que pour le calcul
 * de statistiques de base sur les images.
 */
public class Histogramme 
{

    public static int[] Histogramme256(int mat[][])
    {
        if (mat == null || mat.length == 0 || mat[0].length == 0) {
            System.err.println("Histogramme256: Matrice invalide.");
            return null;
        }

        int M = mat.length;
        int N = mat[0].length;
        int histo[] = new int[256];
        
        for(int i=0 ; i<256 ; i++) histo[i] = 0;
        
        for(int i=0 ; i<M ; i++)
            for(int j=0 ; j<N ; j++)
                if ((mat[i][j] >= 0) && (mat[i][j]<=255)) histo[mat[i][j]]++;
        
        return histo;
    }
// --- NOUVELLES MÉTHODES STATISTIQUES ---

    /**
     * Calcule la valeur minimale des pixels d'une image en niveaux de gris.
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @return La valeur minimale (entre 0 et 255), ou -1 si l'image est invalide.
     */
    public static int minimum(int[][] image) {
        if (image == null || image.length == 0 || image[0].length == 0) return -1;
        int minVal = 256; // Commencer plus haut que le max possible (255)
        for (int[] row : image) {
            for (int pixel : row) {
                if (pixel < minVal) {
                    minVal = pixel;
                }
            }
        }
        // Retourner 0 si minVal n'a jamais été mis à jour (image vide ou hors plage?)
        // Ou retourner la valeur trouvée (peut être > 255 si l'image contient des erreurs)
        // On retourne minVal tel quel, le clamping est fait ailleurs si besoin.
        return (minVal == 256) ? 0 : minVal; // Retourne 0 pour une image vide pour éviter 256
    }

    /**
     * Calcule la valeur maximale des pixels d'une image en niveaux de gris.
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @return La valeur maximale (entre 0 et 255), ou -1 si l'image est invalide.
     */
    public static int maximum(int[][] image) {
        if (image == null || image.length == 0 || image[0].length == 0) return -1;
        int maxVal = -1; // Commencer plus bas que le min possible (0)
        for (int[] row : image) {
            for (int pixel : row) {
                if (pixel > maxVal) {
                    maxVal = pixel;
                }
            }
        }
        return maxVal; // Retourne -1 si l'image est vide
    }

    /**
     * Calcule la luminance (valeur moyenne des pixels) d'une image en niveaux de gris.
     * (Formule 1.28 des notes de cours - Moyenne arithmétique)
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @return La luminance moyenne, ou -1.0 si l'image est invalide.
     */
    public static double luminance(int[][] image) {
        if (image == null || image.length == 0 || image[0].length == 0) return -1.0;
        int hauteur = image.length;
        int largeur = image[0].length;
        long sommePixels = 0; // Utiliser long pour éviter dépassement sur grandes images

        for (int[] row : image) {
            for (int
                    pixel : row) {
                sommePixels += pixel;
            }
        }
        return (double) sommePixels / (hauteur * largeur);
    }

    /**
     * Calcule le contraste (écart-type des pixels) d'une image en niveaux de gris.
     * (Formule 1.29 des notes de cours - Écart-type)
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @return L'écart-type des valeurs de pixels, ou -1.0 si l'image est invalide.
     */
    public static double contraste1(int[][] image) {
        // ... (code inchangé, mais basé sur nbPixelsValides) ...
        if (image == null || image.length == 0 || image[0].length == 0) return -1.0;
        int hauteur = image.length;
        int largeur = image[0].length;
        long nbPixelsValides = 0;
        for(int[] row : image) { for(int p : row) if(p>=0 && p<=255) nbPixelsValides++; }

        if (nbPixelsValides <= 1) return 0.0;

        double moyenne = luminance(image);
        if (moyenne < 0) return -1.0;

        double sommeCarresEcarts = 0.0;
        for (int[] row : image) {
            for (int pixel : row) {
                if (pixel >= 0 && pixel <=255) {
                    sommeCarresEcarts += Math.pow(pixel - moyenne, 2);
                }
            }
        }
        double variance = sommeCarresEcarts / nbPixelsValides; // Variance de la population
        return Math.sqrt(variance);
    }

    /**
     * Calcule le contraste basé sur la variation entre niveaux min et max.
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @return La valeur du contraste C2, ou -1.0 si l'image est invalide ou
     *         si min+max est zéro (image noire).
     * @technique Contraste de Michelson.
     * @see #contraste2(int[][]) Formule 4.30 du cours (C2).
     */
    public static double contraste2(int[][] image) {
        if (image == null || image.length == 0 || image[0].length == 0) return -1.0;
        int minVal = minimum(image);
        int maxVal = maximum(image);

        if (minVal == -1 || maxVal == -1) return -1.0; // Erreur min/max

        double denominateur = (double) maxVal + minVal;
        if (Math.abs(denominateur) < 1e-9) { // Vérifier si max + min est proche de zéro
            if (maxVal == 0) return 0.0; // Si l'image est toute noire, contraste 0
            else return -1.0; // Cas indéfini (ne devrait pas arriver avec min>=0)
        }
        return (double) (maxVal - minVal) / denominateur;
    }
    // --- MÉTHODES DE REHAUSSEMENT ---

    /**
     * Applique une courbe tonale (LUT) à une image pour la rehausser.
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @param courbeTonale Un tableau int[256] où l'indice est le niveau de gris
     *                     d'entrée et la valeur est le niveau de gris de sortie.
     *                     Les valeurs doivent être dans [0, 255].
     * @return La nouvelle image rehaussée (int[hauteur][largeur]), ou null si invalide.
     * @technique Application d'une Look-Up Table (LUT).
     */
    public static int[][] rehaussement(int[][] image, int[] courbeTonale) {
        if (image == null || image.length == 0 || image[0].length == 0) return null;
        if (courbeTonale == null || courbeTonale.length != 256) return null;

        int hauteur = image.length;
        int largeur = image[0].length;
        int[][] resultat = new int[hauteur][largeur];

        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                int pixelValue = image[y][x];
                if (pixelValue >= 0 && pixelValue <= 255) {
                    // Appliquer la LUT
                    resultat[y][x] = courbeTonale[pixelValue];
                    // Sécurité: Clamper le résultat de la LUT (au cas où)
                    // resultat[y][x] = Math.max(0, Math.min(255, courbeTonale[pixelValue]));
                } else {
                    resultat[y][x] = 0; // Ou copier la valeur invalide ? Mettons 0.
                }
            }
        }
        return resultat;
    }

    /**
     * Crée une courbe tonale pour une transformation linéaire avec saturation.
     * @param smin Le seuil minimal d'intensité. Les valeurs < smin seront mises à 0.
     * @param smax Le seuil maximal d'intensité. Les valeurs > smax seront mises à 255.
     * @return La courbe tonale (LUT) int[256].
     * @technique Transformation linéaire par morceaux avec saturation.
     * @see #creeCourbeTonaleLineaireSaturation(int, int) Formule 4.32 du cours.
     */
    public static int[] creeCourbeTonaleLineaireSaturation(int smin, int smax) {
        // Assurer smin <= smax
        if (smin > smax) { int temp = smin; smin = smax; smax = temp; }
        // Assurer que les seuils sont dans [0, 255]
        smin = Math.max(0, Math.min(255, smin));
        smax = Math.max(0, Math.min(255, smax));

        int[] lut = new int[256];
        double range = (double) smax - smin;

        for (int i = 0; i < 256; i++) {
            if (i < smin) {
                lut[i] = 0;
            } else if (i > smax) {
                lut[i] = 255;
            } else {
                // Calcul linéaire entre smin et smax
                if (range < 1e-6) { // Cas où smin == smax
                    lut[i] = (smin == 0) ? 0 : 255; // Ou 128 ? Mettons 0 si smin=0, 255 sinon.
                } else {
                    lut[i] = (int) Math.round(255.0 * (i - smin) / range);
                }
            }
            // Clamping de sécurité (normalement pas nécessaire ici)
            // lut[i] = Math.max(0, Math.min(255, lut[i]));
        }
        return lut;
    }

    /**
     * Crée une courbe tonale pour une correction Gamma.
     * @param gamma Le facteur gamma. gamma > 1 éclaircit, gamma < 1 assombrit (typiquement).
     * @return La courbe tonale (LUT) int[256].
     * @technique Correction Gamma.
     * @see #creeCourbeTonaleGamma(double) Formule 4.33 du cours.
     */
    public static int[] creeCourbeTonaleGamma(double gamma) {
        if (gamma <= 0) gamma = 1.0; // Éviter gamma négatif ou nul

        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) {
            // Appliquer la formule: I' = 255 * (I/255)^gamma
            double valNorm = i / 255.0;
            double correctedVal = 255.0 * Math.pow(valNorm, gamma);
            lut[i] = (int) Math.round(correctedVal);
            // Clamper le résultat
            lut[i] = Math.max(0, Math.min(255, lut[i]));
        }
        return lut;
    }

    /**
     * Crée une courbe tonale pour obtenir le négatif de l'image.
     * @return La courbe tonale (LUT) int[256].
     * @technique Négatif photographique.
     * @see #creeCourbeTonaleNegatif() Formule 4.34 du cours.
     */
    public static int[] creeCourbeTonaleNegatif() {
        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) {
            lut[i] = 255 - i;
        }
        return lut;
    }

    /**
     * Crée une courbe tonale correspondant à l'égalisation de l'histogramme de l'image.
     * @param image L'image d'entrée (int[hauteur][largeur], convention [y][x]).
     * @return La courbe tonale (LUT) int[256], ou null si l'image est invalide.
     * @technique Égalisation d'histogramme.
     * @see #creeCourbeTonaleEgalisation(int[][]) Section 4.4.3 du cours.
     */
    public static int[] creeCourbeTonaleEgalisation(int[][] image) {
        if (image == null || image.length == 0 || image[0].length == 0) return null;
        int hauteur = image.length;
        int largeur = image[0].length;
        long nbPixelsTotal = (long)hauteur * largeur;
        if (nbPixelsTotal == 0) return null;

        // 1. Calculer l'histogramme
        int[] hist = Histogramme256(image);
        if (hist == null) return null;

        // 2. Calculer l'histogramme cumulé (non normalisé pour l'instant)
        long[] histCumul = new long[256];
        histCumul[0] = hist[0];
        for (int i = 1; i < 256; i++) {
            histCumul[i] = histCumul[i - 1] + hist[i];
        }

        // 3. Créer la LUT en appliquant la formule I' = 255 * C(I)
        // où C(I) = histCumul[I] / nbPixelsTotal
        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) {
            // Note: La division doit être faite en double précision
            lut[i] = (int) Math.round(255.0 * histCumul[i] / nbPixelsTotal);
            // Clamper par sécurité (normalement pas nécessaire si histCumul <= nbPixelsTotal)
            // lut[i] = Math.max(0, Math.min(255, lut[i]));
        }

        return lut;
    }
}

