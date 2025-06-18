package ImageProcessing.Seuillage;

import ImageProcessing.Histogramme.Histogramme; // Nécessaire pour Otsu

/**
 * Contient des méthodes statiques pour la segmentation d'images par seuillage.
 */
public class Seuillage {

    /**
     * Réalise un seuillage simple (binarisation) de l'image.
     * Les pixels <= seuil deviennent 0 (noir), les pixels > seuil deviennent 255 (blanc).
     *
     * @param image L'image d'entrée en niveaux de gris (int[hauteur][largeur]).
     * @param seuil La valeur de seuil (typiquement entre 0 et 255).
     * @return Une nouvelle image binaire (int[hauteur][largeur] avec valeurs 0 ou 255).
     *         Retourne null si l'image d'entrée est invalide.
     *
     * @technique Seuillage Simple (Binarisation).
     */
    public static int[][] seuillageSimple(int[][] image, int seuil) {
        if (image == null || image.length == 0 || image[0].length == 0) return null;

        int hauteur = image.length;
        int largeur = image[0].length;
        int[][] resultat = new int[hauteur][largeur];

        // S'assurer que le seuil est dans une plage raisonnable
        // (on pourrait débattre si on autorise <0 ou >255, mais restons simple)
        // int seuilValide = Math.max(0, Math.min(255, seuil));

        System.out.println("Application Seuillage Simple (seuil=" + seuil + ")...");

        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                // Appliquer la condition de seuillage
                resultat[y][x] = (image[y][x] > seuil) ? 255 : 0;
            }
        }
        return resultat;
    }

    /**
     * Réalise un seuillage multiple avec deux seuils.
     * Crée une image avec 3 niveaux de gris distincts.
     * - Pixels <= seuil1      -> niveauBas (ex: 0)
     * - seuil1 < Pixels <= seuil2 -> niveauMoyen (ex: 128)
     * - Pixels > seuil2      -> niveauHaut (ex: 255)
     *
     * @param image L'image d'entrée en niveaux de gris (int[hauteur][largeur]).
     * @param seuil1 Le premier seuil (inférieur).
     * @param seuil2 Le deuxième seuil (supérieur). Doit être >= seuil1.
     * @return Une nouvelle image avec 3 niveaux (int[hauteur][largeur]).
     *         Retourne null si l'image d'entrée est invalide ou si seuil1 > seuil2.
     *
     * @technique Seuillage Multiple (Double Seuil).
     */
    public static int[][] seuillageDouble(int[][] image, int seuil1, int seuil2) {
        if (image == null || image.length == 0 || image[0].length == 0) return null;
        if (seuil1 > seuil2) {
            System.err.println("Erreur [seuillageDouble]: seuil1 doit être <= seuil2.");
            return null;
        }

        int hauteur = image.length;
        int largeur = image[0].length;
        int[][] resultat = new int[hauteur][largeur];

        // Définir les niveaux de sortie (on peut les choisir)
        final int NIVEAU_BAS = 0;
        final int NIVEAU_MOYEN = 128;
        final int NIVEAU_HAUT = 255;

        System.out.println("Application Seuillage Double (seuils=" + seuil1 + ", " + seuil2 + ")...");

        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                int pixel = image[y][x];
                if (pixel <= seuil1) {
                    resultat[y][x] = NIVEAU_BAS;
                } else if (pixel <= seuil2) { // pixel > seuil1 ET pixel <= seuil2
                    resultat[y][x] = NIVEAU_MOYEN;
                } else { // pixel > seuil2
                    resultat[y][x] = NIVEAU_HAUT;
                }
            }
        }
        return resultat;
    }

    /**
     * Réalise un seuillage automatique en utilisant l'algorithme d'Otsu
     * pour déterminer le seuil optimal qui minimise la variance intra-classe.
     * Applique ensuite un seuillage simple avec ce seuil.
     *
     * @param image L'image d'entrée en niveaux de gris (int[hauteur][largeur]).
     * @return Une nouvelle image binaire (int[hauteur][largeur] avec valeurs 0 ou 255).
     *         Retourne null si l'image d'entrée est invalide.
     *
     * @technique Seuillage Automatique, Méthode d'Otsu. Section 1.6.4 des notes.
     */
    public static int[][] seuillageAutomatique(int[][] image) {
        int[] histogramme = Histogramme.Histogramme256(image);
        int seuil = 127;
        int nouveauSeuil = -1;
        while (seuil != nouveauSeuil) {
            int sommeClasse1 = 0, effectifClasse1 = 0;
            int sommeClasse2 = 0, effectifClasse2 = 0;
            for (int i = 0; i <= seuil; i++) {
                sommeClasse1 += i * histogramme[i];
                effectifClasse1 += histogramme[i];
            }
            for (int i = seuil + 1; i < 256; i++) {
                sommeClasse2 += i * histogramme[i];
                effectifClasse2 += histogramme[i];
            }
            int moyenneClasse1 = (effectifClasse1 == 0) ? 0 : sommeClasse1 / effectifClasse1;
            int moyenneClasse2 = (effectifClasse2 == 0) ? 0 : sommeClasse2 / effectifClasse2;
            nouveauSeuil = seuil;
            seuil = (moyenneClasse1 + moyenneClasse2) / 2;
        }
        return seuillageSimple(image, seuil);
    }
}