package isilimageprocessing;

import CImage.*;
import CImage.Exceptions.*;
import CImage.Observers.*;
import CImage.Observers.Events.*;
import ImageProcessing.Complexe.MatriceComplexe;
import ImageProcessing.Contours.ContoursLineaire;
import ImageProcessing.Contours.ContoursNonLineaire;
import ImageProcessing.Core.ImageUtils;
import ImageProcessing.Fourier.Fourier;
import ImageProcessing.Histogramme.Histogramme;
import ImageProcessing.Lineaire.FiltrageLinaireGlobal;
import ImageProcessing.Lineaire.FiltrageLineaireLocal;
import ImageProcessing.NonLineaire.MorphoComplexe;
import ImageProcessing.NonLineaire.MorphoElementaire;
import ImageProcessing.Seuillage.Seuillage;
import isilimageprocessing.Dialogues.*;
import java.awt.*;
import java.io.*;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import java.awt.Color;

/**
 *
 * @author  HP_Propri�taire
 */
public class IsilImageProcessing extends javax.swing.JFrame implements ClicListener,SelectLigneListener,SelectRectListener,SelectRectFillListener,SelectCercleListener,SelectCercleFillListener
{
    private CImageRGB imageRGB;
    private CImageNG  imageNG;
    
    private JLabelBeanCImage observer;
    private Color couleurPinceauRGB;
    private int   couleurPinceauNG;

    // ---> AJOUTER CETTE DÉCLARATION <---
    private JMenu menuFiltrageLineaire;
    // Note : Les autres JMenu (jMenuDessiner, jMenuFourier, etc.) sont déclarés
    // plus bas comme "private javax.swing.JMenu jMenuDessiner;" etc.
    // par le designer GUI. C'est ok, mais pour un menu ajouté manuellement,
    // il faut le déclarer soi-même ici.
    private int[][]   originalImageMatrix; // <-- NOUVEAU: Pour stocker l'original (version NG)
    private final JMenuItem itemRevenirOriginal;        // Référence à l'item de menu


    // ---> NOUVELLE VARIABLE MEMBRE <---
    private JMenu menuTraitementNonLineaire;
    /** Enumération pour identifier les opérations morphologiques élémentaires */
    private enum MorphoOperation {
        EROSION, DILATATION, OUVERTURE, FERMETURE
    }
    // ---> NOUVELLE VARIABLE POUR L'IMAGE SECONDAIRE <---
    private int[][] secondaryImageMatrix = null; // Pour stocker le marqueur ou le masque
    private String secondaryImageName = null;   // Nom du fichier (optionnel, pour info)

    // --- Menu Contours ---
    private JMenu menuContours ;
    private JMenu menuSeuillage;

    // ---Appliaction Menu
    private JMenu menuApplications;

    /** Creates new form TestCImage2 */
    public IsilImageProcessing() {
        initComponents();

        imageRGB = null;
        imageNG  = null;

        observer = new JLabelBeanCImage();
        // ... configuration de observer ...
        jScrollPane.setViewportView(observer);

        // Désactiver les menus au démarrage
        jMenuDessiner.setEnabled(false);
        jMenuFourier.setEnabled(false);
        jMenuHistogramme.setEnabled(false);


        couleurPinceauRGB = Color.BLACK;
        couleurPinceauNG = 0;



        //*******APPLICATION******


        menuApplications = new JMenu("Applications");
        jMenuBar1.add(menuApplications); // Ajouter à la barre de menus

        JMenuItem itemEx1 = new JMenuItem("Ex 1: Débruitage Poivre & Sel...");
        itemEx1.addActionListener(e -> handleExercice1());
        JMenuItem itemEx2 = new JMenuItem("Ex 2: Rehausser l'image...");
        itemEx2.addActionListener(e -> handleExercice2());
        JMenuItem itemEx3 = new JMenuItem("Ex 3: Separer Rouge et bleu...");
        itemEx3.addActionListener(e -> handleExercice3());
        JMenuItem itemEx4 = new JMenuItem("Ex 4: Classification balanes..");
        itemEx4.addActionListener(e -> handleExercice4());
        JMenuItem itemEx5 = new JMenuItem("Ex 5: Recuperation d'objet..");
        itemEx5.addActionListener(e -> handleExercice5());
        JMenuItem itemEx6 = new JMenuItem("Ex 6: vaiseaux..");
        itemEx6.addActionListener(e -> handleExercice6());
        JMenuItem itemEx7 = new JMenuItem("Ex 7: Detection tartines..");
        itemEx7.addActionListener(e -> handleExercice7());

        menuApplications.add(itemEx1);
        menuApplications.add(itemEx2);
        menuApplications.add(itemEx3);
        menuApplications.add(itemEx4);
        menuApplications.add(itemEx5);
        menuApplications.add(itemEx6);
        menuApplications.add(itemEx7);
        menuApplications.setEnabled(true); // On peut toujours lancer les exos



        // --- Menu Contours ---
        menuContours = new JMenu("Contours");
        jMenuBar1.add(menuContours); // Ajouter à la barre de menus

// --- Sous-Menu Linéaire (Contours) ---
        JMenu subMenuContourLineaire = new JMenu("Linéaire");
        menuContours.add(subMenuContourLineaire);

// --- Items pour les gradients et Laplaciens ---

        JMenuItem itemPrewittH = new JMenuItem("Gradient Prewitt (Horizontal)");
        itemPrewittH.addActionListener(e -> handleGradient("Prewitt Horizontal", 1, true)); // dir=1, isPrewitt=true
        subMenuContourLineaire.add(itemPrewittH);

        JMenuItem itemPrewittV = new JMenuItem("Gradient Prewitt (Vertical)");
        itemPrewittV.addActionListener(e -> handleGradient("Prewitt Vertical", 2, true)); // dir=2, isPrewitt=true
        subMenuContourLineaire.add(itemPrewittV);

        JMenuItem itemSobelH = new JMenuItem("Gradient Sobel (Horizontal)");
        itemSobelH.addActionListener(e -> handleGradient("Sobel Horizontal", 1, false)); // dir=1, isPrewitt=false
        subMenuContourLineaire.add(itemSobelH);

        JMenuItem itemSobelV = new JMenuItem("Gradient Sobel (Vertical)");
        itemSobelV.addActionListener(e -> handleGradient("Sobel Vertical", 2, false)); // dir=2, isPrewitt=false
        subMenuContourLineaire.add(itemSobelV);

        subMenuContourLineaire.add(new JSeparator()); // Séparateur

        JMenuItem itemLaplacien4 = new JMenuItem("Laplacien (4-connexité)");
        itemLaplacien4.addActionListener(e -> handleLaplacien("Laplacien 4", true)); // isLaplacien4=true
        subMenuContourLineaire.add(itemLaplacien4);

        JMenuItem itemLaplacien8 = new JMenuItem("Laplacien (8-connexité)");
        itemLaplacien8.addActionListener(e -> handleLaplacien("Laplacien 8", false)); // isLaplacien4=false
        subMenuContourLineaire.add(itemLaplacien8);


        // --- Sous-Menu Non-Linéaire (Contours) ---
        JMenu subMenuContourNonLineaire = new JMenu("Non-linéaire");
        menuContours.add(subMenuContourNonLineaire);

        // --- Items pour les opérateurs non-linéaires ---

        JMenuItem itemGradErosion = new JMenuItem("Gradient (Érosion)");
        itemGradErosion.addActionListener(e -> handleContourNonLineaire("Gradient Érosion", 1)); // type=1
        subMenuContourNonLineaire.add(itemGradErosion);

        JMenuItem itemGradDilatation = new JMenuItem("Gradient (Dilatation)");
        itemGradDilatation.addActionListener(e -> handleContourNonLineaire("Gradient Dilatation", 2)); // type=2
        subMenuContourNonLineaire.add(itemGradDilatation);

        JMenuItem itemGradBeucher = new JMenuItem("Gradient (Beucher)");
        itemGradBeucher.addActionListener(e -> handleContourNonLineaire("Gradient Beucher", 3)); // type=3
        subMenuContourNonLineaire.add(itemGradBeucher);

        JMenuItem itemLaplacienNL = new JMenuItem("Laplacien Non-Linéaire");
        itemLaplacienNL.addActionListener(e -> handleContourNonLineaire("Laplacien Non-Linéaire", 4)); // type=4
        subMenuContourNonLineaire.add(itemLaplacienNL);


        menuContours.setEnabled(false);
        // --- Item "Afficher Paramètres..." ---
        JMenuItem itemAfficherParams = new JMenuItem("Afficher Paramètres...");
        itemAfficherParams.addActionListener(e -> {
            String operationName = "Afficher Paramètres";
            if (imageNG == null && imageRGB == null) { JOptionPane.showMessageDialog(this, "Veuillez charger une image.", "Aucune image", JOptionPane.WARNING_MESSAGE); return; }
            CImage imageSource = (imageNG != null) ? imageNG : imageRGB;

            try {
                System.out.println("Calcul des paramètres...");
                int[][] matrix = ImageUtils.imageToGrayMatrix(imageSource);
                if (matrix == null) throw new RuntimeException("Erreur conversion image.");

                int min = Histogramme.minimum(matrix);
                int max = Histogramme.maximum(matrix);
                double lum = Histogramme.luminance(matrix);
                double c1 = Histogramme.contraste1(matrix);
                double c2 = Histogramme.contraste2(matrix);

                // Formatage pour affichage
                DecimalFormat df = new DecimalFormat("#0.00"); // 2 décimales
                StringBuilder message = new StringBuilder("<html><body>"); // Utiliser HTML pour les sauts de ligne
                message.append("<b>Paramètres de l'image :</b><br>");
                message.append("Minimum : ").append(min).append("<br>");
                message.append("Maximum : ").append(max).append("<br>");
                message.append("Luminance (Moyenne) : ").append(df.format(lum)).append("<br>");
                message.append("Contraste 1 (Écart-type) : ").append(df.format(c1)).append("<br>");
                message.append("Contraste 2 (Michelson) : ").append(df.format(c2)).append("<br>");
                message.append("</body></html>");

                JOptionPane.showMessageDialog(this, message.toString(), "Paramètres de l'image", JOptionPane.INFORMATION_MESSAGE);
                System.out.println("Paramètres affichés.");

            } catch (Exception ex) {
                handleProcessingError(operationName, ex);
            }
        });
        jMenuHistogramme.add(itemAfficherParams); // Ajouter au menu existant

        // --- Séparateur et Sous-Menu Rehaussement ---
        jMenuHistogramme.add(new JSeparator());

        JMenu subMenuRehaussement = new JMenu("Rehaussement par Courbe Tonale");
        jMenuHistogramme.add(subMenuRehaussement);

        // --- Items de Rehaussement ---

        // -- Item Linéaire (Min/Max) --
        JMenuItem itemLinMinMax = new JMenuItem("Linéaire (Min/Max)");
        itemLinMinMax.addActionListener(e -> applyLutTransformation("Linéaire (Min/Max)",
                (matrix) -> { // Lambda pour créer la LUT
                    int min = Histogramme.minimum(matrix);
                    int max = Histogramme.maximum(matrix);
                    if (min == -1 || max == -1) return null; // Gérer erreur min/max
                    return Histogramme.creeCourbeTonaleLineaireSaturation(min, max);
                }
        ));
        subMenuRehaussement.add(itemLinMinMax);

        // -- Item Linéaire Saturation... --
        JMenuItem itemLinSaturation = new JMenuItem("Linéaire Saturation...");
        itemLinSaturation.addActionListener(e -> {
            String operationName = "Linéaire Saturation";
            if (imageNG == null && imageRGB == null) { JOptionPane.showMessageDialog(this, "Veuillez charger une image.", "Aucune image", JOptionPane.WARNING_MESSAGE); return; }

            // Dialogue pour Smin et Smax
            JTextField sminField = new JTextField(5);
            JTextField smaxField = new JTextField(5);
            JPanel panelSat = new JPanel(new GridLayout(2, 2, 5, 5));
            panelSat.add(new JLabel("Smin (0-255):"));
            panelSat.add(sminField);
            panelSat.add(new JLabel("Smax (0-255):"));
            panelSat.add(smaxField);

            int result = JOptionPane.showConfirmDialog(this, panelSat, operationName, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    int smin = Integer.parseInt(sminField.getText().trim());
                    int smax = Integer.parseInt(smaxField.getText().trim());
                    // Ajouter validation si besoin (ex: smin <= smax, 0-255)

                    applyLutTransformation(operationName,
                            (matrix) -> Histogramme.creeCourbeTonaleLineaireSaturation(smin, smax)
                    );
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Erreur saisie: Smin et Smax doivent être des entiers.", "Erreur Paramètres", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    handleProcessingError(operationName, ex);
                }
            }
        });
        subMenuRehaussement.add(itemLinSaturation);

        // -- Item Gamma... --
        JMenuItem itemGamma = new JMenuItem("Correction Gamma...");
        itemGamma.addActionListener(e -> {
            String operationName = "Correction Gamma";
            if (imageNG == null && imageRGB == null) { JOptionPane.showMessageDialog(this, "Veuillez charger une image.", "Aucune image", JOptionPane.WARNING_MESSAGE); return; }

            String gammaStr = JOptionPane.showInputDialog(this, "Facteur Gamma (ex: 0.5, 1.8):", operationName, JOptionPane.QUESTION_MESSAGE);
            if (gammaStr == null || gammaStr.trim().isEmpty()) return;

            try {
                double gamma = Double.parseDouble(gammaStr.trim());
                if (gamma <= 0) throw new NumberFormatException("Gamma doit être positif.");

                applyLutTransformation(operationName,
                        (matrix) -> Histogramme.creeCourbeTonaleGamma(gamma)
                );
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Erreur saisie: " + ex.getMessage(), "Erreur Paramètre", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                handleProcessingError(operationName, ex);
            }
        });
        subMenuRehaussement.add(itemGamma);

        // -- Item Négatif --
        JMenuItem itemNegatif = new JMenuItem("Négatif");
        itemNegatif.addActionListener(e -> applyLutTransformation("Négatif",
                (matrix) -> Histogramme.creeCourbeTonaleNegatif()
        ));
        subMenuRehaussement.add(itemNegatif);

        // -- Item Égalisation --
        JMenuItem itemEgalisation = new JMenuItem("Égalisation Histogramme");
        itemEgalisation.addActionListener(e -> {
            String operationName = "Égalisation Histogramme";
            // Note: L'affichage avant/après est géré dans la méthode helper
            applyLutTransformation(operationName,
                    (matrix) -> Histogramme.creeCourbeTonaleEgalisation(matrix),
                    true // Activer l'affichage avant/après histogramme
            );
        });
        subMenuRehaussement.add(itemEgalisation);

        // --- AJOUT DU MENU FILTRAGE LINÉAIRE ---
        menuFiltrageLineaire = new JMenu("Filtrage linéaire"); // Assigne à la variable membre
        jMenuBar1.add(menuFiltrageLineaire, 3);

        // --- Sous-Menu Local ---
        JMenu subMenuLocal = new JMenu("Local");
        menuFiltrageLineaire.add(subMenuLocal);

        // -- Item Convolution --
        JMenuItem itemConvolution = new JMenuItem("Convolution Masque...");
        itemConvolution.addActionListener(e -> {
            if (imageNG == null && imageRGB == null) { JOptionPane.showMessageDialog(this, "Veuillez charger une image.", "Aucune image", JOptionPane.WARNING_MESSAGE); return; }
            JOptionPane.showMessageDialog(this, "Fonctionnalité 'Convolution Masque' à implémenter (saisie du masque).");
            // Mettre ici le code de test ou la logique de saisie/parsing du masque plus tard
        });
        subMenuLocal.add(itemConvolution);

        // -- Item Moyenneur --
        JMenuItem itemMoyenneur = new JMenuItem("Filtre Moyenneur...");
        itemMoyenneur.addActionListener(e -> {
            if (imageNG == null && imageRGB == null) { JOptionPane.showMessageDialog(this, "Veuillez charger une image.", "Aucune image", JOptionPane.WARNING_MESSAGE); return; }
            CImage imageSource = (imageNG != null) ? imageNG : imageRGB;

            String tailleStr = JOptionPane.showInputDialog(this, "Taille masque moyenneur (entier impair):", "Filtre Moyenneur", JOptionPane.QUESTION_MESSAGE);
            if (tailleStr == null || tailleStr.trim().isEmpty()) return;

            try {
                int tailleMasque = Integer.parseInt(tailleStr.trim());
                if (tailleMasque <= 0 || tailleMasque % 2 == 0) throw new NumberFormatException("Taille invalide (doit être entier positif impair).");

                System.out.println("Application Filtre Moyenneur...");
                int[][] inputMatrix = ImageUtils.imageToGrayMatrix(imageSource);
                if (inputMatrix == null) throw new RuntimeException("Erreur conversion image.");

                int[][] resultMatrix = FiltrageLineaireLocal.filtreMoyenneur(inputMatrix, tailleMasque);
                if (resultMatrix == null) throw new RuntimeException("Erreur filtre moyenneur.");

                CImageNG resultCImage = ImageUtils.matrixToCImageNG(resultMatrix);
                if (resultCImage == null) throw new RuntimeException("Erreur conversion résultat.");

                updateImageDisplay(resultCImage); // Utiliser une méthode pour mettre à jour
                System.out.println("Filtre Moyenneur appliqué.");

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Erreur saisie: " + ex.getMessage(), "Erreur Paramètre", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                handleProcessingError("Filtre Moyenneur", ex);
            }
        });
        subMenuLocal.add(itemMoyenneur);

        // --- Sous-Menu Global ---
        JMenu subMenuGlobal = new JMenu("Global");
        menuFiltrageLineaire.add(subMenuGlobal);

        // -- Item Passe-Bas Idéal --
        JMenuItem itemPasseBasIdeal = new JMenuItem("Passe-Bas Idéal...");
        itemPasseBasIdeal.addActionListener(e -> handleIdealFilter(true)); // true = LowPass
        subMenuGlobal.add(itemPasseBasIdeal);

        // -- Item Passe-Haut Idéal --
        JMenuItem itemPasseHautIdeal = new JMenuItem("Passe-Haut Idéal...");
        itemPasseHautIdeal.addActionListener(e -> handleIdealFilter(false)); // false = HighPass
        subMenuGlobal.add(itemPasseHautIdeal);

        // -- Item Passe-Bas Butterworth --
        JMenuItem itemPasseBasButter = new JMenuItem("Passe-Bas Butterworth...");
        itemPasseBasButter.addActionListener(e -> handleButterworthFilter(true)); // true = LowPass
        subMenuGlobal.add(itemPasseBasButter);

        // -- Item Passe-Haut Butterworth --
        JMenuItem itemPasseHautButter = new JMenuItem("Passe-Haut Butterworth...");
        itemPasseHautButter.addActionListener(e -> handleButterworthFilter(false)); // false = HighPass
        subMenuGlobal.add(itemPasseHautButter);

        // ---> AJOUT DU MENU TRAITEMENT NON-LINÉAIRE <---
        menuTraitementNonLineaire = new JMenu("Traitement non-linéaire");
        // Ajouter à la barre de menu (par exemple, après Filtrage Linéaire)
        jMenuBar1.add(menuTraitementNonLineaire, 4);

        // --- Sous-Menu Élémentaire ---
        JMenu subMenuElementaire = new JMenu("Élémentaire");
        menuTraitementNonLineaire.add(subMenuElementaire);

        // -- Item Érosion --
        JMenuItem itemErosion = new JMenuItem("Érosion...");
        itemErosion.addActionListener(e -> handleMorphoOperation(MorphoOperation.EROSION));
        subMenuElementaire.add(itemErosion);

        // -- Item Dilatation --
        JMenuItem itemDilatation = new JMenuItem("Dilatation...");
        itemDilatation.addActionListener(e -> handleMorphoOperation(MorphoOperation.DILATATION));
        subMenuElementaire.add(itemDilatation);

        // -- Item Ouverture --
        JMenuItem itemOuverture = new JMenuItem("Ouverture...");
        itemOuverture.addActionListener(e -> handleMorphoOperation(MorphoOperation.OUVERTURE));
        subMenuElementaire.add(itemOuverture);

        // -- Item Fermeture --
        JMenuItem itemFermeture = new JMenuItem("Fermeture...");
        itemFermeture.addActionListener(e -> handleMorphoOperation(MorphoOperation.FERMETURE));
        subMenuElementaire.add(itemFermeture);

        // --- Désactiver le menu au démarrage ---
        menuTraitementNonLineaire.setEnabled(false);

        // --- Désactiver le menu au démarrage ---
        menuFiltrageLineaire.setEnabled(false);
// ---> AJOUT DU SOUS-MENU COMPLEXE ET ITEM MÉDIAN <---
        JMenu subMenuComplexe = new JMenu("Complexe");
        menuTraitementNonLineaire.add(subMenuComplexe); // Ajoute au menu Non-Linéaire

        // -- Item Filtre Médian --
        JMenuItem itemFiltreMedian = new JMenuItem("Filtre Médian...");
        // ---> AJOUT DES ITEMS GÉODÉSIQUES <---

        // -- Item Dilatation Géodésique (Itérative) --
        JMenuItem itemDilaGeo = new JMenuItem("Dilatation Géodésique (Marqueur=Masque)...");
        itemDilaGeo.addActionListener(e -> handleGeodesicOperation(true)); // true = Dilatation itérative
        subMenuComplexe.add(itemDilaGeo);

        // -- Item Reconstruction Géodésique (Convergence) --
        JMenuItem itemReconsGeo = new JMenuItem("Reconstruction Géodésique (Marqueur=Masque)"); // Pas besoin de "..." car pas d'input user
        itemReconsGeo.addActionListener(e -> handleGeodesicOperation(false)); // false = Reconstruction
        subMenuComplexe.add(itemReconsGeo);

        itemFiltreMedian.addActionListener(e -> {
            // Logique similaire aux autres filtres simples
            String operationName = "Filtre Médian";
            if (imageNG == null && imageRGB == null) { JOptionPane.showMessageDialog(this, "Veuillez charger une image.", "Aucune image", JOptionPane.WARNING_MESSAGE); return; }
            CImage imageSource = (imageNG != null) ? imageNG : imageRGB;

            String tailleStr = JOptionPane.showInputDialog(this, "Taille du voisinage carré (entier impair):", operationName, JOptionPane.QUESTION_MESSAGE);
            if (tailleStr == null || tailleStr.trim().isEmpty()) return;

            try {
                int tailleMasque = Integer.parseInt(tailleStr.trim());
                if (tailleMasque <= 0 || tailleMasque % 2 == 0) throw new NumberFormatException("Taille invalide (doit être entier positif impair).");

                System.out.println("Application " + operationName + "...");
                int[][] inputMatrix = ImageUtils.imageToGrayMatrix(imageSource);
                if (inputMatrix == null) throw new RuntimeException("Erreur conversion image.");

                // Appel de la nouvelle fonction
                int[][] resultMatrix = ImageProcessing.NonLineaire.MorphoComplexe.filtreMedian(inputMatrix, tailleMasque);

                if (resultMatrix == null) throw new RuntimeException("Erreur pendant l'opération " + operationName + ".");

                CImageNG resultCImage = ImageUtils.matrixToCImageNG(resultMatrix);
                if (resultCImage == null) throw new RuntimeException("Erreur conversion résultat.");

                updateImageDisplay(resultCImage); // Mettre à jour l'affichage
                System.out.println("Opération " + operationName + " appliquée.");

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Erreur saisie: " + ex.getMessage(), "Erreur Paramètre", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                handleProcessingError(operationName, ex);
            }
        });
        subMenuComplexe.add(itemFiltreMedian); // Ajoute l'item au sous-menu Complexe
        // Dans le constructeur, ajouter cet item au menu jMenuImage:
        JMenuItem itemLoadSecondary = new JMenuItem("Charger Image Secondaire (Marqueur/Masque)...");
        itemLoadSecondary.addActionListener(e -> {
            JFileChooser choix = new JFileChooser();
            choix.setCurrentDirectory(new File("."));
            // Optionnel: Ajouter un filtre pour n'afficher que les images
            // FileFilter imageFilter = new FileNameExtensionFilter("Images", "jpg", "png", "gif", "bmp", "jpeg");
            // choix.setFileFilter(imageFilter);

            if (choix.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File fichier = choix.getSelectedFile();
                if (fichier != null) {
                    try {
                        System.out.println("Chargement de l'image secondaire: " + fichier.getName());
                        // On charge comme CImage pour utiliser imageToGrayMatrix qui gère NG/RGB
                        CImage tempCImage;
                        try { // Essayer de charger comme NG d'abord (plus direct si c'est le cas)
                            tempCImage = new CImageNG(fichier);
                        } catch (Exception exNg) { // Si échec, essayer comme RGB
                            try {
                                tempCImage = new CImageRGB(fichier);
                            } catch (Exception exRgb) {
                                // Si les deux échouent, relancer une exception
                                throw new IOException("Impossible de charger l'image comme NG ou RGB.", exRgb);
                            }
                        }

                        secondaryImageMatrix = ImageUtils.imageToGrayMatrix(tempCImage);
                        if (secondaryImageMatrix == null) {
                            throw new RuntimeException("Erreur lors de la conversion de l'image secondaire.");
                        }
                        secondaryImageName = fichier.getName();
                        JOptionPane.showMessageDialog(this,
                                "Image secondaire '" + secondaryImageName + "' chargée.",
                                "Info Image Secondaire", JOptionPane.INFORMATION_MESSAGE);
                        // Optionnel: Mettre à jour une barre de statut ou une étiquette
                        // statusBarLabel.setText("Image secondaire chargée: " + secondaryImageName);

                    } catch (IOException ex) {
                        secondaryImageMatrix = null;
                        secondaryImageName = null;
                        JOptionPane.showMessageDialog(this, "Erreur lors du chargement de l'image secondaire:\n" + ex.getMessage(), "Erreur Chargement", JOptionPane.ERROR_MESSAGE);
                    } catch (Exception ex) { // Autres erreurs potentielles
                        secondaryImageMatrix = null;
                        secondaryImageName = null;
                        handleProcessingError("Charger Image Secondaire", ex);
                    }
                }
            }
        });
        jMenuImage.add(itemLoadSecondary); // Ajouter au menu Image



        // Dans le constructeur, après avoir ajouté jMenuItemEnregistrerSous :
        jMenuImage.add(new JSeparator()); // Séparateur optionnel
        itemRevenirOriginal = new JMenuItem("Revenir à l'original");
        itemRevenirOriginal.setEnabled(false); // Désactivé au début
        itemRevenirOriginal.addActionListener(e -> {
            if (originalImageMatrix != null) {
                System.out.println("Retour à l'image originale...");
                CImageNG originalAsNG = ImageUtils.matrixToCImageNG(originalImageMatrix);
                if (originalAsNG != null) {
                    updateImageDisplay(originalAsNG); // Met à jour affichage et menus
                    // Optionnel: Désactiver "Revenir à l'original" car on y est déjà ?
                    // itemRevenirOriginal.setEnabled(false);
                } else {
                    handleProcessingError("Revenir à l'original", new RuntimeException("Erreur re-conversion matrice originale"));
                }
            }
        });
        jMenuImage.add(itemRevenirOriginal);
        jMenuImage.add(jSeparator1); // Le séparateur avant Quitter

        // --- Menu Seuillage ---
        menuSeuillage = new JMenu("Seuillage");
        jMenuBar1.add(menuSeuillage); // Ajouter à la barre de menus

        // --- Items de Seuillage ---

        JMenuItem itemSeuilSimple = new JMenuItem("Seuillage Simple...");
        itemSeuilSimple.addActionListener(e -> handleSeuillageSimple());
        menuSeuillage.add(itemSeuilSimple);

        JMenuItem itemSeuilDouble = new JMenuItem("Seuillage Double...");
        itemSeuilDouble.addActionListener(e -> handleSeuillageDouble());
        menuSeuillage.add(itemSeuilDouble);

        JMenuItem itemSeuilAuto = new JMenuItem("Seuillage Automatique (Otsu)"); // Pas de "..." car pas de paramètre user
        itemSeuilAuto.addActionListener(e -> handleSeuillageAutomatique());
        menuSeuillage.add(itemSeuilAuto);
        menuSeuillage.setEnabled(false);

    } // Fin du constructeur


    // *********APPLICATION EXERCICES

    // Ajouter la méthode suivante dans la classe IsilImageProcessing :
    private void handleExercice1()  {
        String operationName = "Exercice 1: Débruitage Poivre & Sel";

        // 1. Choisir le fichier
        JFileChooser choix = new JFileChooser();
        choix.setCurrentDirectory(new File(".")); // Commence dans le répertoire courant
        choix.setDialogTitle(operationName + " - Choisir une image bruitée");
        // Optionnel: Mettre un filtre pour n'afficher que les images bruitee1/2 ?
        // choix.setFileFilter(new FileNameExtensionFilter("Images Bruitee", "png", "jpg"));
        int retour = choix.showOpenDialog(this);

        if (retour == JFileChooser.APPROVE_OPTION) {
            File fichier = choix.getSelectedFile();
            if (fichier == null || !fichier.exists()) {
                JOptionPane.showMessageDialog(this, "Fichier invalide.", "Erreur Fichier", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // 2. Charger l'image choisie
                System.out.println("Chargement image pour Ex1: " + fichier.getName());
                CImage imageSource;
                try { // Essayer NG d'abord
                    imageSource = new CImageNG(fichier);
                } catch (Exception exNg) {
                    try { // Essayer RGB ensuite
                        imageSource = new CImageRGB(fichier);
                    } catch (Exception exRgb) {
                        throw new IOException("Impossible de charger l'image comme NG ou RGB.", exRgb);
                    }
                }

                // Stocker comme original si on veut pouvoir y revenir
                originalImageMatrix = ImageUtils.imageToGrayMatrix(imageSource);
                updateRevenirOriginalMenuState(); // Mettre à jour le menu Revenir

                // 3. Convertir en matrice
                int[][] inputMatrix = ImageUtils.imageToGrayMatrix(imageSource); // Utiliser la copie stockée ? Non, utiliser imageSource directement.
                if (inputMatrix == null) throw new RuntimeException("Erreur conversion image.");

                // 4. Demander la taille du filtre médian
                String tailleStr = JOptionPane.showInputDialog(this,
                        "Entrez la taille du voisinage pour le filtre Médian (ex: 3, 5 - entier impair):",
                        operationName, JOptionPane.QUESTION_MESSAGE);
                if (tailleStr == null || tailleStr.trim().isEmpty()) return; // Annulation

                int tailleVoisinage = Integer.parseInt(tailleStr.trim());
                if (tailleVoisinage <= 0 || tailleVoisinage % 2 == 0) {
                    throw new NumberFormatException("La taille doit être un entier positif impair.");
                }

                // 5. Appliquer le filtre Médian
                System.out.println("Application du Filtre Médian (taille=" + tailleVoisinage + ")...");
                int[][] resultMatrix = ImageProcessing.NonLineaire.MorphoComplexe.filtreMedian(inputMatrix, tailleVoisinage);
                if (resultMatrix == null) {
                    throw new RuntimeException("Erreur lors de l'application du filtre médian.");
                }

                // 6. Convertir et afficher
                CImageNG resultCImage = ImageUtils.matrixToCImageNG(resultMatrix);
                if (resultCImage == null) {
                    throw new RuntimeException("Erreur conversion résultat.");
                }
                updateImageDisplay(resultCImage); // Met à jour l'affichage

                System.out.println(operationName + " terminé.");
                JOptionPane.showMessageDialog(this, "Débruitage par filtre médian appliqué.", operationName, JOptionPane.INFORMATION_MESSAGE);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Erreur de saisie : " + ex.getMessage(), "Erreur Paramètre", JOptionPane.ERROR_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Erreur de chargement du fichier:\n" + ex.getMessage(), "Erreur Fichier", JOptionPane.ERROR_MESSAGE);
                handleProcessingError(operationName, ex); // Log l'erreur aussi
            } catch (Exception ex) {
                handleProcessingError(operationName, ex);
            }
        } else {
            System.out.println(operationName + ": Chargement annulé.");
        }
    }

    //execice 2
    private void handleExercice2() {
        String operationName = "Exercice 2: Égalisation Lena Couleur";
        String filename = "lenaAEgaliser.jpg"; // Nom de fichier imposé

        // 1. Vérifier si le fichier existe
        File inputFile = new File(filename);
        if (!inputFile.exists()) {
            JOptionPane.showMessageDialog(this,
                    "Le fichier '" + filename + "' est introuvable.\nAssurez-vous qu'il est à la racine du projet.",
                    "Erreur Fichier", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // 2. Charger l'image couleur
            System.out.println("Chargement de " + filename + "...");
            CImageRGB imageRGB_Orig = new CImageRGB(inputFile);

            // Stocker l'original (version NG) pour pouvoir y revenir
            originalImageMatrix = ImageUtils.imageToGrayMatrix(imageRGB_Orig);
            updateRevenirOriginalMenuState();

            int hauteur = imageRGB_Orig.getHauteur();
            int largeur = imageRGB_Orig.getLargeur();

            // --- Méthode (a): Égalisation RGB Indépendante ---
            System.out.println("Début Méthode (a): Égalisation RGB Indépendante...");

            // 2a. Obtenir les matrices R, G, B
            // IMPORTANT: Supposons que getMatricesRGB retourne [largeur][hauteur] ([x][y])
            // Nos fonctions Histogramme attendent [hauteur][largeur] ([y][x])
            // Il faut donc transposer.
            int[][] rOrig_xy = new int[largeur][hauteur];
            int[][] gOrig_xy = new int[largeur][hauteur];
            int[][] bOrig_xy = new int[largeur][hauteur];
            imageRGB_Orig.getMatricesRGB(rOrig_xy, gOrig_xy, bOrig_xy); // Récupère dans [x][y]

            // Transposer en [y][x] pour nos fonctions
            int[][] rOrig_yx = transposeMatrix(rOrig_xy);
            int[][] gOrig_yx = transposeMatrix(gOrig_xy);
            int[][] bOrig_yx = transposeMatrix(bOrig_xy);
            if (rOrig_yx == null || gOrig_yx == null || bOrig_yx == null) throw new RuntimeException("Erreur de transposition.");

            // 3a. Calculer et appliquer LUT pour chaque canal
            System.out.println("  Égalisation canal R...");
            int[] lutR = Histogramme.creeCourbeTonaleEgalisation(rOrig_yx);
            int[][] rEq_yx = Histogramme.rehaussement(rOrig_yx, lutR);

            System.out.println("  Égalisation canal G...");
            int[] lutG = Histogramme.creeCourbeTonaleEgalisation(gOrig_yx);
            int[][] gEq_yx = Histogramme.rehaussement(gOrig_yx, lutG);

            System.out.println("  Égalisation canal B...");
            int[] lutB = Histogramme.creeCourbeTonaleEgalisation(bOrig_yx);
            int[][] bEq_yx = Histogramme.rehaussement(bOrig_yx, lutB);

            if (rEq_yx == null || gEq_yx == null || bEq_yx == null) throw new RuntimeException("Erreur pendant l'égalisation/rehaussement d'un canal.");

            // 4a. Re-Transposer en [x][y] pour CImageRGB et créer l'image résultat A
            int[][] rEq_xy = transposeMatrix(rEq_yx);
            int[][] gEq_xy = transposeMatrix(gEq_yx);
            int[][] bEq_xy = transposeMatrix(bEq_yx);
            if (rEq_xy == null || gEq_xy == null || bEq_xy == null) throw new RuntimeException("Erreur de re-transposition.");

            System.out.println("  Création image résultat A...");
            CImageRGB resultA_CImageRGB = new CImageRGB(rEq_xy, gEq_xy, bEq_xy);

            // 5a. Sauvegarder le résultat A
            String outputA = "lena_egalisation_A_RGB.png";
            System.out.println("  Sauvegarde " + outputA + "...");
            resultA_CImageRGB.enregistreFormatPNG(new File(outputA));


            // --- Méthode (b): Égalisation Luminance + Application LUT Y aux canaux ---
            System.out.println("Début Méthode (b): Égalisation via Luminance...");

            // 2b. Calculer la matrice de Luminance [y][x]
            System.out.println("  Calcul Luminance...");
            int[][] lum_yx = ImageUtils.imageToGrayMatrix(imageRGB_Orig); // Utilise la fonction qui fait déjà la conversion et donne [y][x]
            if (lum_yx == null) throw new RuntimeException("Erreur calcul luminance.");

            // 3b. Calculer la LUT d'égalisation basée sur la Luminance
            System.out.println("  Calcul LUT d'égalisation (Y)...");
            int[] lutY = Histogramme.creeCourbeTonaleEgalisation(lum_yx);
            if (lutY == null) throw new RuntimeException("Erreur création LUT luminance.");

            // 4b. Appliquer CETTE MÊME LUT (lutY) aux canaux R, G, B ORIGINAUX [y][x]
            System.out.println("  Application LUT(Y) sur R, G, B...");
            int[][] rEqY_yx = Histogramme.rehaussement(rOrig_yx, lutY); // Applique lutY sur R original [y][x]
            int[][] gEqY_yx = Histogramme.rehaussement(gOrig_yx, lutY); // Applique lutY sur G original [y][x]
            int[][] bEqY_yx = Histogramme.rehaussement(bOrig_yx, lutY); // Applique lutY sur B original [y][x]
            if (rEqY_yx == null || gEqY_yx == null || bEqY_yx == null) throw new RuntimeException("Erreur pendant l'application de LUT(Y) aux canaux.");

            // 5b. Re-Transposer en [x][y] pour CImageRGB et créer l'image résultat B
            int[][] rEqY_xy = transposeMatrix(rEqY_yx);
            int[][] gEqY_xy = transposeMatrix(gEqY_yx);
            int[][] bEqY_xy = transposeMatrix(bEqY_yx);
            if (rEqY_xy == null || gEqY_xy == null || bEqY_xy == null) throw new RuntimeException("Erreur de re-transposition (B).");

            System.out.println("  Création image résultat B...");
            CImageRGB resultB_CImageRGB = new CImageRGB(rEqY_xy, gEqY_xy, bEqY_xy);

            // 6b. Sauvegarder le résultat B
            String outputB = "lena_egalisation_B_Luminance.png";
            System.out.println("  Sauvegarde " + outputB + "...");
            resultB_CImageRGB.enregistreFormatPNG(new File(outputB));

            // 7. Afficher un des résultats (par exemple B, souvent meilleur) et informer
            System.out.println("Affichage du résultat de la méthode B (Luminance)...");
            // Convertir B en NG pour l'affichage standard
            CImageNG resultB_CImageNG = ImageUtils.matrixToCImageNG(ImageUtils.imageToGrayMatrix(resultB_CImageRGB));
            if (resultB_CImageNG == null) throw new RuntimeException("Erreur conversion résultat B en NG.");
            updateImageDisplay(resultB_CImageNG);

            JOptionPane.showMessageDialog(this,
                    "Égalisation terminée.\n" +
                            "Résultat méthode (a) sauvegardé dans : " + outputA + "\n" +
                            "Résultat méthode (b) sauvegardé dans : " + outputB + "\n\n" +
                            "Comparez les deux fichiers. La méthode (b) préserve généralement mieux les couleurs.",
                    operationName, JOptionPane.INFORMATION_MESSAGE);

        } catch (CImageRGBException | IOException exIO) {
            JOptionPane.showMessageDialog(this, "Erreur I/O ou CImage: " + exIO.getMessage(), "Erreur Chargement/Sauvegarde", JOptionPane.ERROR_MESSAGE);
            handleProcessingError(operationName, exIO);
        } catch (Exception ex) {
            handleProcessingError(operationName, ex);
        }
    }

    //exercice 3
    private void handleExercice3() {
        String operationName = "Exercice 3: Segmentation Petits Pois";
        String filename = "petitsPois.png"; // Nom de fichier imposé

        File inputFile = new File(filename);
        if (!inputFile.exists()) {
            JOptionPane.showMessageDialog(this, "Le fichier '" + filename + "' est introuvable.", "Erreur Fichier", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // 1. Charger l'image couleur
            System.out.println("Chargement de " + filename + "...");
            CImageRGB imageRGB_Orig = new CImageRGB(inputFile);

            originalImageMatrix = ImageUtils.imageToGrayMatrix(imageRGB_Orig); // Version NG pour "revenir"
            updateRevenirOriginalMenuState();

            int hauteur = imageRGB_Orig.getHauteur();
            int largeur = imageRGB_Orig.getLargeur();

            // 2. Obtenir les matrices R, G, B [y][x]
            int[][] r_xy = new int[largeur][hauteur];
            int[][] g_xy = new int[largeur][hauteur];
            int[][] b_xy = new int[largeur][hauteur];
            imageRGB_Orig.getMatricesRGB(r_xy, g_xy, b_xy);

            int[][] r_yx = transposeMatrix(r_xy);
            int[][] g_yx = transposeMatrix(g_xy);
            int[][] b_yx = transposeMatrix(b_xy);
            if (r_yx == null || g_yx == null || b_yx == null) throw new RuntimeException("Erreur de transposition.");

            // 3. Segmenter les POIS ROUGES
            System.out.println("Segmentation des pois rouges...");
            int[][] redBinary = new int[hauteur][largeur];
            int seuilRougeHaut = 150; // À AJUSTER EMPIRIQUEMENT
            int seuilBleuBas = 100;  // À AJUSTER EMPIRIQUEMENT
            int seuilVertBas = 100;  // À AJUSTER EMPIRIQUEMENT

            for (int y = 0; y < hauteur; y++) {
                for (int x = 0; x < largeur; x++) {
                    // Condition simple: R élevé ET B faible ET G faible
                    if (r_yx[y][x] > seuilRougeHaut && b_yx[y][x] < seuilBleuBas && g_yx[y][x] < seuilVertBas) {
                        redBinary[y][x] = 255; // Pixel fait partie d'un pois rouge
                    } else {
                        redBinary[y][x] = 0;
                    }
                }
            }

            // 4. Nettoyer l'image binaire des pois rouges (Ex: Ouverture puis Fermeture)
            System.out.println("  Nettoyage morphologique par Reconstruction (Rouge)...");
            int tailleErosionMarqueur = 7; // À AJUSTER : Assez grand pour supprimer le bruit
            int[][] marqueurRouge = MorphoElementaire.erosion(redBinary, tailleErosionMarqueur);
            if (marqueurRouge == null) {
                System.err.println("Attention: érosion pour marqueur rouge a échoué.");
                // Que faire? Utiliser le résultat binaire brut ou l'ancien nettoyage ?
                // Pour tester, on peut utiliser redBinary, mais ce n'est pas idéal.
                marqueurRouge = redBinary; // Fallback très simple
            }
            int[][] redCleaned = MorphoComplexe.reconstructionGeodesique(marqueurRouge, redBinary); // Masque = binaire initial
            if (redCleaned == null) {
                System.err.println("Attention: reconstruction rouge a échoué, utilisation image binaire brute.");
                redCleaned = redBinary; // Fallback
            }

            // 5. Sauvegarder et/ou Afficher les pois rouges
            String outputRed = "petitsPois_Rouges_seg.png";
            System.out.println("  Sauvegarde " + outputRed + "...");
            CImageNG redResultCImage = ImageUtils.matrixToCImageNG(redCleaned != null ? redCleaned : redBinary);
            if (redResultCImage != null) redResultCImage.enregistreFormatPNG(new File(outputRed));
            else System.err.println("Erreur conversion/sauvegarde pois rouges.");

            // 6. Segmenter les POIS BLEUS (logique similaire)
            System.out.println("Segmentation des pois bleus...");
            int[][] blueBinary = new int[hauteur][largeur];
            int seuilBleuHaut = 150; // À AJUSTER EMPIRIQUEMENT
            int seuilRougeBas = 100;  // À AJUSTER EMPIRIQUEMENT
            // Le seuil Vert peut être moins discriminant ici

            for (int y = 0; y < hauteur; y++) {
                for (int x = 0; x < largeur; x++) {
                    // Condition simple: B élevé ET R faible
                    if (b_yx[y][x] > seuilBleuHaut && r_yx[y][x] < seuilRougeBas) {
                        blueBinary[y][x] = 255;
                    } else {
                        blueBinary[y][x] = 0;
                    }
                }
            }

            // 7. Nettoyer l'image binaire des pois bleus
            System.out.println("  Nettoyage morphologique par Reconstruction (Bleu)...");
            // Utiliser la même taille d'érosion ou une autre si besoin
            int[][] marqueurBleu = MorphoElementaire.erosion(blueBinary, tailleErosionMarqueur);
            if (marqueurBleu == null) {
                System.err.println("Attention: érosion pour marqueur bleu a échoué.");
                marqueurBleu = blueBinary;
            }
            int[][] blueCleaned = MorphoComplexe.reconstructionGeodesique(marqueurBleu, blueBinary); // Masque = binaire initial
            if (blueCleaned == null) {
                System.err.println("Attention: reconstruction bleue a échoué, utilisation image binaire brute.");
                blueCleaned = blueBinary;
            }


            // 8. Sauvegarder et/ou Afficher les pois bleus
            String outputBlue = "petitsPois_Bleus_seg.png";
            System.out.println("  Sauvegarde " + outputBlue + "...");
            CImageNG blueResultCImage = ImageUtils.matrixToCImageNG(blueCleaned != null ? blueCleaned : blueBinary);
            if (blueResultCImage != null) blueResultCImage.enregistreFormatPNG(new File(outputBlue));
            else System.err.println("Erreur conversion/sauvegarde pois bleus.");

            // 9. Afficher un des résultats (ex: pois rouges) et informer
            System.out.println("Affichage du résultat pour les pois rouges...");
            if (redResultCImage != null) {
                updateImageDisplay(redResultCImage);
                JOptionPane.showMessageDialog(this,
                        "Segmentation terminée.\n" +
                                "Résultat pois rouges sauvegardé dans : " + outputRed + "\n" +
                                "Résultat pois bleus sauvegardé dans : " + outputBlue,
                        operationName, JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Erreur lors de la segmentation des pois rouges.", "Erreur", JOptionPane.ERROR_MESSAGE);
            }

        } catch (CImageRGBException | IOException exIO) {
            JOptionPane.showMessageDialog(this, "Erreur I/O ou CImage: " + exIO.getMessage(), "Erreur Fichier", JOptionPane.ERROR_MESSAGE);
            handleProcessingError(operationName, exIO);
        } catch (Exception ex) {
            handleProcessingError(operationName, ex);
        }
    }

    //Exercice 4 :
    private void handleExercice4() {
        String operationName = "Exercice 4: Séparation Balanes";
        String filename = "balanes.png"; // Nom de fichier imposé

        File inputFile = new File(filename);
        if (!inputFile.exists()) {
            JOptionPane.showMessageDialog(this, "Le fichier '" + filename + "' est introuvable.", "Erreur Fichier", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // 1. Charger l'image (sera convertie en NG)
            System.out.println("Chargement de " + filename + "...");
            // On la charge directement en NG si possible, sinon RGB puis convertit
            CImage imageSource;
            try { imageSource = new CImageNG(inputFile); }
            catch (Exception exNg) {
                try { imageSource = new CImageRGB(inputFile); } // Fallback RGB
                catch (Exception exRgb) { throw new IOException("Impossible de charger l'image.", exRgb); }
            }

            int[][] imageOriginaleNG = ImageUtils.imageToGrayMatrix(imageSource);
            if (imageOriginaleNG == null) throw new RuntimeException("Erreur conversion image originale.");

            originalImageMatrix = imageOriginaleNG; // Stocker pour "revenir"
            updateRevenirOriginalMenuState();

            int hauteur = imageOriginaleNG.length;
            int largeur = imageOriginaleNG[0].length;

            // 2. Seuillage initial pour obtenir les formes binaires
            System.out.println("  Seuillage automatique (Otsu)...");
            int[][] imageBinaire = Seuillage.seuillageAutomatique(imageOriginaleNG);
            if (imageBinaire == null) throw new RuntimeException("Erreur pendant le seuillage Otsu.");

            // Optionnel: Nettoyage léger de l'image binaire (ex: petite fermeture pour boucher trous)
            int tailleSE_nettoyage = 3;
            System.out.println("  Nettoyage binaire (Fermeture " + tailleSE_nettoyage + "x" + tailleSE_nettoyage + ")...");
            int[][] imageBinaireNettoyee = MorphoElementaire.fermeture(imageBinaire, tailleSE_nettoyage);
            if (imageBinaireNettoyee == null) imageBinaireNettoyee = imageBinaire; // Fallback

            // 3. Créer un marqueur en érodant fortement pour isoler les centres des grandes
            // --> LA TAILLE DE L'ES EST CRUCIALE ET DOIT ETRE AJUSTEE <--
            int tailleES_Marqueur = 21; // EXEMPLE, À AJUSTER : assez grand pour tuer les petites
            System.out.println("  Création marqueur grandes balanes (Érosion " + tailleES_Marqueur + "x" + tailleES_Marqueur + ")...");
            int[][] marqueurGrandes = MorphoElementaire.erosion(imageBinaireNettoyee, tailleES_Marqueur);
            if (marqueurGrandes == null) throw new RuntimeException("Erreur pendant l'érosion pour marqueur.");

            // [Optionnel mais utile: sauvegarder le marqueur pour voir ce qu'il contient]
            // CImageNG marqueurImg = ImageUtils.matrixToCImageNG(marqueurGrandes);
            // if (marqueurImg != null) marqueurImg.enregistreFormatPNG(new File("balanes_marqueur_debug.png"));


            // 4. Reconstruire les GRANDES balanes à partir du marqueur sous le masque binaire nettoyé
            System.out.println("  Reconstruction des grandes balanes...");
            int[][] grandesBalanesBinaires = MorphoComplexe.reconstructionGeodesique(marqueurGrandes, imageBinaireNettoyee);
            if (grandesBalanesBinaires == null) throw new RuntimeException("Erreur pendant la reconstruction géodésique.");


// 5. Extraction des PETITES balanes par soustraction
            System.out.println("  Extraction petites balanes (Soustraction)...");
            int[][] petitesBalanesBinaires_Bruitees = soustraireImagesBinaires(imageBinaireNettoyee, grandesBalanesBinaires); // Renommé pour clarté
            if (petitesBalanesBinaires_Bruitees == null) throw new RuntimeException("Erreur pendant la soustraction pour petites balanes.");

            // ---> ÉTAPE 5b: Nettoyer l'image des petites balanes <---
            // --> AJUSTER CETTE TAILLE <--
            int tailleES_NettoyagePetites = 5; // ESSAI AVEC 5 (si 3 laissait du bruit)
            System.out.println("  Nettoyage des petites balanes (Ouverture " + tailleES_NettoyagePetites + "x" + tailleES_NettoyagePetites + ")...");
            int[][] petitesBalanesBinaires = MorphoElementaire.ouverture(petitesBalanesBinaires_Bruitees, tailleES_NettoyagePetites);
            if (petitesBalanesBinaires == null) {
                System.err.println("Attention: ouverture petites balanes a échoué, utilisation image bruitée.");
                petitesBalanesBinaires = petitesBalanesBinaires_Bruitees;
            }

            // 6. Création des images finales en NIVEAUX DE GRIS (Utilise maintenant petitesBalanesBinaires nettoyée)
            System.out.println("  Récupération niveaux de gris...");
            int[][] grandesBalanesNG = appliquerMasqueNG(imageOriginaleNG, grandesBalanesBinaires);
            int[][] petitesBalanesNG = appliquerMasqueNG(imageOriginaleNG, petitesBalanesBinaires); // Utilise la version nettoyée
            if (grandesBalanesNG == null || petitesBalanesNG == null) throw new RuntimeException("Erreur pendant l'application des masques NG.");

            // 7. Sauvegarder les résultats
            String outputGrandes = "balanes_Grandes_NG.png";
            String outputPetites = "balanes_Petites_NG.png";
            System.out.println("  Sauvegarde " + outputGrandes + " et " + outputPetites + "...");

            CImageNG grandesResultCImage = ImageUtils.matrixToCImageNG(grandesBalanesNG);
            if (grandesResultCImage != null) grandesResultCImage.enregistreFormatPNG(new File(outputGrandes));
            else System.err.println("Erreur conversion/sauvegarde grandes balanes.");

            CImageNG petitesResultCImage = ImageUtils.matrixToCImageNG(petitesBalanesNG);
            if (petitesResultCImage != null) petitesResultCImage.enregistreFormatPNG(new File(outputPetites));
            else System.err.println("Erreur conversion/sauvegarde petites balanes.");


            // 8. Afficher un des résultats et informer
            System.out.println("Affichage du résultat pour les grandes balanes...");
            if (grandesResultCImage != null) {
                updateImageDisplay(grandesResultCImage);
                JOptionPane.showMessageDialog(this,
                        "Séparation terminée.\n" +
                                "Grandes balanes (NG) sauvegardées dans : " + outputGrandes + "\n" +
                                "Petites balanes (NG) sauvegardées dans : " + outputPetites,
                        operationName, JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Erreur lors de la séparation des grandes balanes.", "Erreur", JOptionPane.ERROR_MESSAGE);
            }

        } catch (IOException exIO) {
            JOptionPane.showMessageDialog(this, "Erreur I/O ou CImage: " + exIO.getMessage(), "Erreur Fichier/Image", JOptionPane.ERROR_MESSAGE);
            handleProcessingError(operationName, exIO);
        } catch (Exception ex) {
            handleProcessingError(operationName, ex);
        }
    }

    //Exercice 5
    private void handleExercice5() {
        String operationName = "Exercice 5: Segmentation Outils";
        String filename = "tools.png"; // Nom de fichier imposé

        File inputFile = new File(filename);
        if (!inputFile.exists()) {
            JOptionPane.showMessageDialog(this, "Le fichier '" + filename + "' est introuvable.", "Erreur Fichier", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // 1. Charger l'image (sera convertie en NG)
            System.out.println("Chargement de " + filename + "...");
            CImage imageSource;
            try { imageSource = new CImageNG(inputFile); }
            catch (Exception exNg) {
                try { imageSource = new CImageRGB(inputFile); } // Fallback RGB
                catch (Exception exRgb) { throw new IOException("Impossible de charger l'image.", exRgb); }
            }

            int[][] imageOriginaleNG = ImageUtils.imageToGrayMatrix(imageSource);
            if (imageOriginaleNG == null) throw new RuntimeException("Erreur conversion image originale.");

            originalImageMatrix = imageOriginaleNG; // Stocker pour "revenir"
            updateRevenirOriginalMenuState();

            // 2. Estimer le fond par Ouverture morphologique
            // --> LA TAILLE DE L'ES EST CRUCIALE ET DOIT ETRE AJUSTEE <--
            int tailleES_Fond = 21; // EXEMPLE: Doit être plus grand que le plus grand outil
            System.out.println("  Estimation du fond (Ouverture " + tailleES_Fond + "x" + tailleES_Fond + ")...");
            int[][] fondEstime = MorphoElementaire.ouverture(imageOriginaleNG, tailleES_Fond);
            if (fondEstime == null) throw new RuntimeException("Erreur pendant l'estimation du fond (ouverture).");

            // [Optionnel mais utile: sauvegarder le fond estimé]
            // CImageNG fondImg = ImageUtils.matrixToCImageNG(fondEstime);
            // if (fondImg != null) fondImg.enregistreFormatPNG(new File("tools_fond_estime_debug.png"));


            // 3. Corriger l'illumination (Top-Hat morphologique)
            // imageCorrigee = imageOriginale - fondEstime
            System.out.println("  Correction de l'illumination (Top-Hat)...");
            int[][] imageCorrigee = soustraireImagesNG(imageOriginaleNG, fondEstime); // Attention: crée une nouvelle méthode soustraireImagesNG
            if (imageCorrigee == null) throw new RuntimeException("Erreur pendant la correction (soustraction).");

            // [Optionnel mais utile: sauvegarder l'image corrigée]
            // CImageNG corrigeeImg = ImageUtils.matrixToCImageNG(imageCorrigee);
            // if (corrigeeImg != null) corrigeeImg.enregistreFormatPNG(new File("tools_corrigee_debug.png"));


            // 4. Seuillage de l'image corrigée
            // Un seuil bas devrait suffire car le fond est maintenant proche de 0
            // Otsu peut marcher aussi, mais un seuil manuel est peut-être plus simple.
            int seuilManuel = 17; // EXEMPLE, À AJUSTER après avoir vu l'image corrigée
            System.out.println("  Seuillage de l'image corrigée (Seuil=" + seuilManuel + ")...");
            // int[][] outilsBinaires = Seuillage.seuillageAutomatique(imageCorrigee); // Essayer Otsu ?
            int[][] outilsBinaires = Seuillage.seuillageSimple(imageCorrigee, seuilManuel); // Ou simple
            if (outilsBinaires == null) throw new RuntimeException("Erreur pendant le seuillage de l'image corrigée.");

// 5. Nettoyage final
            int tailleSE_fermeture = 3; // Pour boucher les trous (ou 5?)
            System.out.println("  Nettoyage final (Fermeture " + tailleSE_fermeture + "x" + tailleSE_fermeture + ")...");
            int[][] outilsBinairesFermes = MorphoElementaire.fermeture(outilsBinaires, tailleSE_fermeture);
            if (outilsBinairesFermes == null) outilsBinairesFermes = outilsBinaires; // Fallback

            // ---> AJOUT: Ouverture pour enlever le bruit "poivre" <---
            int tailleSE_ouverture = 3; // Pour supprimer les petits points (ou 5?)
            System.out.println("  Nettoyage final (Ouverture " + tailleSE_ouverture + "x" + tailleSE_ouverture + ")...");
            int[][] outilsBinairesNettoyes = MorphoElementaire.ouverture(outilsBinairesFermes, tailleSE_ouverture);
            if (outilsBinairesNettoyes == null) outilsBinairesNettoyes = outilsBinairesFermes; // Fallback


            // 6. Sauvegarder le résultat binaire final (utilise outilsBinairesNettoyes)
            String outputFinal = "tools_Segmentation_Binaire.png";
            System.out.println("  Sauvegarde " + outputFinal + "...");
            CImageNG finalResultCImage = ImageUtils.matrixToCImageNG(outilsBinairesNettoyes); // Utilise le résultat final
            if (finalResultCImage != null) finalResultCImage.enregistreFormatPNG(new File(outputFinal));
            else System.err.println("Erreur conversion/sauvegarde résultat final.");

            // 7. Afficher le résultat et informer
            System.out.println("Affichage du résultat de la segmentation...");
            if (finalResultCImage != null) {
                updateImageDisplay(finalResultCImage);
                JOptionPane.showMessageDialog(this,
                        "Segmentation terminée.\n" +
                                "Résultat binaire sauvegardé dans : " + outputFinal,
                        operationName, JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Erreur lors de la segmentation des outils.", "Erreur", JOptionPane.ERROR_MESSAGE);
            }

        } catch (IOException exIO) {
            JOptionPane.showMessageDialog(this, "Erreur I/O ou CImage: " + exIO.getMessage(), "Erreur Fichier/Image", JOptionPane.ERROR_MESSAGE);
            handleProcessingError(operationName, exIO);
        } catch (Exception ex) {
            handleProcessingError(operationName, ex);
        }
    }

    //Execice 6

    private void handleExercice6() {
        String operationName = "Exercice 6: Composition Vaisseau/Planète";
        String vaisseauFile = "vaisseaux.jpg";
        String planeteFile = "planete.jpg";
        String outputSynthese1 = "synthese.png";
        String outputSynthese2 = "synthese2.png";

        File vaisseauInputFile = new File(vaisseauFile);
        File planeteInputFile = new File(planeteFile);

        if (!vaisseauInputFile.exists()) {
            JOptionPane.showMessageDialog(this, "Le fichier '" + vaisseauFile + "' est introuvable.", "Erreur Fichier", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!planeteInputFile.exists()) {
            JOptionPane.showMessageDialog(this, "Le fichier '" + planeteFile + "' est introuvable.", "Erreur Fichier", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // --- Partie 1: Créer le masque du petit vaisseau ---
            System.out.println("Étape 1: Création du masque du petit vaisseau...");

            // 1a. Charger vaisseaux.jpg (source RGB) ET convertir en NG [y][x] pour segmentation
            System.out.println("  Chargement et conversion " + vaisseauFile + "...");
            CImageRGB vaisseauxRGB_source = new CImageRGB(vaisseauInputFile); // Garder l'original RGB pour plus tard
            int[][] vaisseauxNG = ImageUtils.imageToGrayMatrix(vaisseauxRGB_source); // Convertir en NG [y][x]
            if (vaisseauxNG == null) throw new RuntimeException("Erreur conversion vaisseaux NG.");
            // Dimensions basées sur l'image NG (convention y, x)
            int hauteurV_NG = vaisseauxNG.length;
            int largeurV_NG = vaisseauxNG[0].length;

            // 1b. Seuillage pour séparer vaisseaux du fond
            System.out.println("  Seuillage vaisseaux...");
            int[][] vaisseauxBinaires = Seuillage.seuillageAutomatique(vaisseauxNG); // Utilise la matrice NG
            if (vaisseauxBinaires == null) throw new RuntimeException("Erreur seuillage vaisseaux.");

             // 1c. Nettoyage optionnel (petite fermeture peut aider) - ON L'ENLÈVE POUR TESTER
 System.out.println("  Nettoyage binaire initial (Fermeture 3x3)...");
 int[][] vaisseauxBinairesNettoyes = MorphoElementaire.fermeture(vaisseauxBinaires, 3);
 if (vaisseauxBinairesNettoyes == null) vaisseauxBinairesNettoyes = vaisseauxBinaires; // Fallback
          //  int[][] vaisseauxBinairesNettoyes = vaisseauxBinaires; // Utiliser directement le résultat du seuillage

            // 1d-i. Créer un marqueur pour le GROS vaisseau en érodant fortement
            // --> AJUSTER CETTE TAILLE POUR TUER LE PETIT VAISSEAU ET LE TEXTE <--
            int tailleES_MarqueurGros = 41; // EXEMPLE, À AJUSTER ! Doit survivre seulement sur le gros vaisseau.
            System.out.println("  Création marqueur gros vaisseau (Érosion " + tailleES_MarqueurGros + "x" + tailleES_MarqueurGros + ")...");
            int[][] marqueurGros = MorphoElementaire.erosion(vaisseauxBinairesNettoyes, tailleES_MarqueurGros);
            if (marqueurGros == null) throw new RuntimeException("Erreur pendant l'érosion pour marqueur gros vaisseau.");

            // [Optionnel: Sauvegarder le marqueur pour debug]
            // CImageNG marqueurGrosImg = ImageUtils.matrixToCImageNG(marqueurGros);
            // if (marqueurGrosImg != null) marqueurGrosImg.enregistreFormatPNG(new File("vaisseau_gros_marqueur_debug.png"));

            // 1d-ii. Reconstruire le GROS vaisseau à partir du marqueur sous le masque binaire total
            System.out.println("  Reconstruction du gros vaisseau...");
            int[][] grosVaisseauBinaire = MorphoComplexe.reconstructionGeodesique(marqueurGros, vaisseauxBinairesNettoyes); // Masque = binaire total
            if (grosVaisseauBinaire == null) throw new RuntimeException("Erreur pendant la reconstruction du gros vaisseau.");

            // [Optionnel: Sauvegarder le masque reconstruit du gros vaisseau pour debug]
            // CImageNG grosVReconsImg = ImageUtils.matrixToCImageNG(grosVaisseauBinaire);
            // if (grosVReconsImg != null) grosVReconsImg.enregistreFormatPNG(new File("vaisseau_gros_reconstruit_debug.png"));

            // 1d-iii. Obtenir le PETIT vaisseau (et autres éléments non reconstruits) par soustraction
            System.out.println("  Extraction masque petit vaisseau (Soustraction)...");
            int[][] petitVaisseauEtBruitBinaire = soustraireImagesBinaires(vaisseauxBinairesNettoyes, grosVaisseauBinaire);
            if (petitVaisseauEtBruitBinaire == null) throw new RuntimeException("Erreur soustraction petit vaisseau.");

            // 1d-iv. Nettoyer le masque du petit vaisseau (Optionnel mais recommandé)
            // Appliquer une OUVERTURE pour supprimer le texte/bruit qui pourrait rester,
            // en gardant une taille d'ES plus petite que le petit vaisseau.
            int tailleES_NettoyagePetit = 9; // EXEMPLE, À AJUSTER: Doit enlever le texte mais garder le petit vaisseau
            System.out.println("  Nettoyage masque petit vaisseau (Ouverture " + tailleES_NettoyagePetit + "x" + tailleES_NettoyagePetit + ")...");
            int[][] petitVaisseauBinaire = MorphoElementaire.ouverture(petitVaisseauEtBruitBinaire, tailleES_NettoyagePetit);
            if (petitVaisseauBinaire == null) {
                System.err.println("Attention: ouverture nettoyage petit vaisseau a échoué, utilisation masque bruité.");
                petitVaisseauBinaire = petitVaisseauEtBruitBinaire; // Fallback
            }

            // [Optionnel: Sauvegarder le masque final du petit vaisseau pour debug]
            CImageNG masquePetitVFinal = ImageUtils.matrixToCImageNG(petitVaisseauBinaire);
            if (masquePetitVFinal != null) masquePetitVFinal.enregistreFormatPNG(new File("vaisseau_petit_masque_final_debug.png"));

            // --- Partie 2: Copier/Coller via BufferedImage ---
            System.out.println("Étape 2: Copier/Coller le vaisseau via BufferedImage...");

            // 2a. Charger l'image de la planète (cible RGB) et obtenir sa BufferedImage
            System.out.println("  Chargement " + planeteFile + "...");
            CImageRGB planeteRGB_cible = new CImageRGB(planeteInputFile);
            BufferedImage biPlanete = planeteRGB_cible.getImage();
            if (biPlanete == null) throw new RuntimeException("Erreur: BufferedImage planète null.");
            int hauteurP = biPlanete.getHeight();
            int largeurP = biPlanete.getWidth();

            // Obtenir la BufferedImage de la source (vaisseaux)
            BufferedImage biVaisseaux = vaisseauxRGB_source.getImage();
            if (biVaisseaux == null) throw new RuntimeException("Erreur: BufferedImage vaisseaux null.");
            int hauteurV_BI = biVaisseaux.getHeight(); // Dimensions de la BufferedImage
            int largeurV_BI = biVaisseaux.getWidth();


            // 2b. Créer la BufferedImage résultat (une copie de la planète)
            System.out.println("  Création BufferedImage résultat (copie planète)...");
            BufferedImage biSynthese1 = new BufferedImage(largeurP, hauteurP, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = biSynthese1.createGraphics();
            g2d.drawImage(biPlanete, 0, 0, null);
            g2d.dispose();
            System.out.println("  Copie planète terminée.");

            // 2c. Coller le vaisseau pixel par pixel (rapide avec getRGB/setRGB)
            System.out.println("  Collage du vaisseau (rapide)...");
            int yMax = Math.min(hauteurV_NG, hauteurP); // Utiliser hauteur masque/cible
            int xMax = Math.min(largeurV_NG, largeurP); // Utiliser largeur masque/cible

            for (int y = 0; y < yMax; y++) {
                for (int x = 0; x < xMax; x++) {
                    // Vérifier les limites du masque binaire [y][x]
                    if (y < petitVaisseauBinaire.length && x < petitVaisseauBinaire[0].length && petitVaisseauBinaire[y][x] == 255) {
                        // Vérifier les limites des BufferedImage avant get/set RGB
                        if (x < largeurV_BI && y < hauteurV_BI && x < largeurP && y < hauteurP) {
                            int rgbVaisseau = biVaisseaux.getRGB(x, y);
                            biSynthese1.setRGB(x, y, rgbVaisseau);
                        }
                    }
                }
            }
            System.out.println("  Collage du vaisseau terminé.");

            // 2d. Sauvegarder synthese.png
            System.out.println("  Sauvegarde " + outputSynthese1 + "...");
            ImageIO.write(biSynthese1, "png", new File(outputSynthese1));


            // --- Partie 3: Ajouter le contour rouge ---
            System.out.println("Étape 3: Ajout du contour rouge...");

            // 3a. Calculer le contour du masque binaire
            System.out.println("  Calcul du contour du masque...");
            int[][] contourBinaire = ContoursNonLineaire.gradientBeucher(petitVaisseauBinaire);
            if (contourBinaire == null) throw new RuntimeException("Erreur calcul contour.");

            // 3b. Dessiner le contour en rouge sur biSynthese1
            System.out.println("  Dessin du contour (rapide)...");
            int redRGB = Color.RED.getRGB();
            for (int y = 0; y < yMax; y++) {
                for (int x = 0; x < xMax; x++) {
                    // Vérifier les limites du contour binaire [y][x]
                    if (y < contourBinaire.length && x < contourBinaire[0].length && contourBinaire[y][x] == 255) {
                        // Vérifier les limites de l'image de synthèse
                        if (x < largeurP && y < hauteurP) {
                            biSynthese1.setRGB(x, y, redRGB);
                        }
                    }
                }
            }
            System.out.println("  Dessin contour terminé.");

            // 3c. Sauvegarder synthese2.png
            System.out.println("  Sauvegarde " + outputSynthese2 + "...");
            ImageIO.write(biSynthese1, "png", new File(outputSynthese2));


            // --- Partie 4: Affichage et Fin ---
            System.out.println("Affichage de synthese2.png (avec contour)...");

            // Re-créer un objet CImageNG pour l'affichage à partir de la BufferedImage finale
            // Solution: Créer un CImageRGB temporaire puis utiliser les ImageUtils
            CImageRGB tempSyntheseRGB;
            try {
                tempSyntheseRGB = new CImageRGB(largeurP, hauteurP, 0, 0, 0); // Créer objet CImageRGB
                // Utiliser getContexte() pour dessiner la BufferedImage dedans
                Graphics g = tempSyntheseRGB.getContexte(); // <--- CORRECTION ICI
                if (g != null) {
                    g.drawImage(biSynthese1, 0, 0, null);
                    // Pas besoin de g.dispose() car ce Graphics vient de CImage
                } else {
                    throw new RuntimeException("Impossible d'obtenir le contexte graphique de tempSyntheseRGB.");
                }
            } catch (CImageRGBException e) {
                throw new RuntimeException("Erreur création CImageRGB temporaire: " + e.getMessage());
            }

            CImageNG synthese2Affichage = ImageUtils.matrixToCImageNG(ImageUtils.imageToGrayMatrix(tempSyntheseRGB));

            if (synthese2Affichage != null) {
                updateImageDisplay(synthese2Affichage); // Doit afficher la version NG
                JOptionPane.showMessageDialog(this,
                        "Composition terminée.\n" +
                                "Résultats sauvegardés dans " + outputSynthese1 + " et " + outputSynthese2,
                        operationName, JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Erreur lors de la finalisation de l'exercice 6.", "Erreur", JOptionPane.ERROR_MESSAGE);
            }


        } catch (IOException exIO) {
            JOptionPane.showMessageDialog(this, "Erreur I/O ou CImage: " + exIO.getMessage(), "Erreur Fichier/Image", JOptionPane.ERROR_MESSAGE);
            handleProcessingError(operationName, exIO);
        } catch (Exception ex) {
            handleProcessingError(operationName, ex);
        }
    } // Fin handleExercice6


    /// Exo 7

    private void handleExercice7() {
        String operationName = "Exercice 7: Contours Tartines";
        String filename = "Tartines.jpg"; // Nom de fichier imposé
        String outputFilename = "Tartines_avec_contours.png";

        File inputFile = new File(filename);
        if (!inputFile.exists()) {
            JOptionPane.showMessageDialog(this, "Le fichier '" + filename + "' est introuvable.", "Erreur Fichier", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // 1. Charger l'image originale (RGB pour la modifier, et la convertir en NG)
            System.out.println("Chargement et conversion de " + filename + "...");
            CImageRGB tartinesRGB_Orig = new CImageRGB(inputFile);
            int[][] tartinesNG = ImageUtils.imageToGrayMatrix(tartinesRGB_Orig);
            if (tartinesNG == null) throw new RuntimeException("Erreur conversion image NG.");

            originalImageMatrix = tartinesNG; // Stocker pour "revenir"
            updateRevenirOriginalMenuState();

            int hauteur = tartinesNG.length;
            int largeur = tartinesNG[0].length;
            System.out.println("  Lissage initial (Moyenneur 3x3)...");
            int[][] tartinesNG_Lisse = FiltrageLineaireLocal.filtreMoyenneur(tartinesNG, 3);
            if (tartinesNG_Lisse == null) tartinesNG_Lisse = tartinesNG;

            // 2. Détection des contours (Utilisation du gradient de Beucher suggérée)
            System.out.println("  Détection des contours (Gradient Beucher)...");
            int[][] contoursNG = ContoursNonLineaire.gradientBeucher(tartinesNG_Lisse);
            if (contoursNG == null) throw new RuntimeException("Erreur calcul gradient Beucher.");

            // 3. Binarisation des contours (pour des lignes plus nettes)
            // --> AJUSTER LE SEUIL SI NÉCESSAIRE <--
            int seuilContour = 30; // Seuil pour considérer un pixel comme contour
            System.out.println("  Binarisation des contours (Seuil=" + seuilContour + ")...");
            int[][] contoursBinaires = Seuillage.seuillageSimple(contoursNG, seuilContour);
            if (contoursBinaires == null) throw new RuntimeException("Erreur seuillage contours.");

            // [Optionnel: Sauvegarder les contours binaires pour debug]
            // CImageNG contoursBinImg = ImageUtils.matrixToCImageNG(contoursBinaires);
            // if (contoursBinImg != null) contoursBinImg.enregistreFormatPNG(new File("tartines_contours_bin_debug.png"));


            // 4. Tracer les contours en vert sur l'image originale RGB
            System.out.println("  Traçage des contours en vert...");
            // On repart de l'image RGB originale chargée au début
            CImageRGB tartinesAvecContours = new CImageRGB(inputFile); // Recharger pour être sûr
            Color couleurContour = Color.GREEN;

            for (int y = 0; y < hauteur; y++) {
                for (int x = 0; x < largeur; x++) {
                    // Vérifier les limites et si le pixel est un contour
                    if (y < contoursBinaires.length && x < contoursBinaires[0].length && contoursBinaires[y][x] == 255) {
                        try {
                            tartinesAvecContours.setPixel(x, y, couleurContour);
                        } catch (CImageRGBException ex) {
                            // Ignorer les erreurs de setPixel pour ne pas bloquer
                            // System.err.println("Erreur setPixel contour à [" + x + "," + y + "]: " + ex.getMessage());
                        }
                    }
                }
            }

            // 5. Sauvegarder le résultat final
            System.out.println("  Sauvegarde " + outputFilename + "...");
            tartinesAvecContours.enregistreFormatPNG(new File(outputFilename));

            // 6. Afficher le résultat et informer
            System.out.println("Affichage du résultat avec contours...");
            // Convertir en NG pour l'affichage standard
            CImageNG resultatFinalNG = ImageUtils.matrixToCImageNG(ImageUtils.imageToGrayMatrix(tartinesAvecContours));
            if (resultatFinalNG != null) {
                updateImageDisplay(resultatFinalNG);
                JOptionPane.showMessageDialog(this,
                        "Détection et tracé des contours terminés.\n" +
                                "Résultat sauvegardé dans : " + outputFilename,
                        operationName, JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Erreur lors de la finalisation de l'exercice 7.", "Erreur", JOptionPane.ERROR_MESSAGE);
            }


        } catch (IOException exIO) {
            JOptionPane.showMessageDialog(this, "Erreur I/O ou CImage: " + exIO.getMessage(), "Erreur Fichier/Image", JOptionPane.ERROR_MESSAGE);
            handleProcessingError(operationName, exIO);
        } catch (Exception ex) {
            handleProcessingError(operationName, ex);
        }
    }
    // Assurez-vous que les méthodes soustraireImagesBinaires, updateImageDisplay, handleProcessingError,
    // ImageUtils.*, Seuillage.*, MorphoElementaire.*, ContoursNonLineaire.* existent et fonctionnent.
    // La méthode transposeMatrix n'est pas explicitement nécessaire ici si on utilise getPixel/setPixel
    // et que ImageUtils gère la convention pour les traitements internes.    /**
//     * Méthode utilitaire pour soustraire imageB de imageA (A - B) pour des images NG.
//     * Clampe le résultat à 0 si B > A.
//     */
    private int[][] soustraireImagesNG(int[][] imageA, int[][] imageB) {
        if (imageA == null || imageB == null || imageA.length != imageB.length || imageA[0].length != imageB[0].length) return null;
        int hauteur = imageA.length;
        int largeur = imageA[0].length;
        int[][] resultat = new int[hauteur][largeur];
        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                int diff = imageA[y][x] - imageB[y][x];
                resultat[y][x] = Math.max(0, diff); // Met à 0 si négatif
                // On ne clampe PAS à 255 ici, le résultat peut être > 255 si l'original l'était
                // Le seuillage s'en chargera.
            }
        }
        return resultat;
    }

    /**
     * Méthode utilitaire pour soustraire imageB de imageA (A - B).
     * Utile pour obtenir les petites balanes: BinaireTotal - GrandesBinaires
     * Suppose des images binaires (0 ou 255).
     */
    private int[][] soustraireImagesBinaires(int[][] imageA, int[][] imageB) {
        if (imageA == null || imageB == null || imageA.length != imageB.length || imageA[0].length != imageB[0].length) return null;
        int hauteur = imageA.length;
        int largeur = imageA[0].length;
        int[][] resultat = new int[hauteur][largeur];
        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                resultat[y][x] = (imageA[y][x] == 255 && imageB[y][x] == 0) ? 255 : 0;
            }
        }
        return resultat;
    }

    /**
     * Méthode utilitaire pour appliquer un masque binaire à une image en niveaux de gris.
     * Copie les pixels de l'image NG là où le masque est blanc (255), met 0 ailleurs.
     */
    private int[][] appliquerMasqueNG(int[][] imageNG, int[][] masqueBinaire) {
        if (imageNG == null || masqueBinaire == null || imageNG.length != masqueBinaire.length || imageNG[0].length != masqueBinaire[0].length) return null;
        int hauteur = imageNG.length;
        int largeur = imageNG[0].length;
        int[][] resultat = new int[hauteur][largeur];
        for (int y = 0; y < hauteur; y++) {
            for (int x = 0; x < largeur; x++) {
                resultat[y][x] = (masqueBinaire[y][x] == 255) ? imageNG[y][x] : 0;
            }
        }
        return resultat;
    }




    /**
     * Méthode factorisée pour appliquer une transformation basée sur une courbe tonale (LUT).
     * Gère la conversion, l'appel au créateur de LUT, l'application, et la mise à jour.
     *
     * @param operationName Nom de l'opération (pour les logs/erreurs).
     * @param lutCreator Fonction qui prend la matrice d'entrée et retourne la LUT (int[256]).
     */
    private void applyLutTransformation(String operationName, java.util.function.Function<int[][], int[]> lutCreator) {
        applyLutTransformation(operationName, lutCreator, false); // Par défaut, ne pas montrer les histos
    }

    /**
     * Surcharge pour inclure l'option d'affichage des histogrammes avant/après.
     */
    private void applyLutTransformation(String operationName, java.util.function.Function<int[][], int[]> lutCreator, boolean showHistograms) {
        if (imageNG == null && imageRGB == null) { JOptionPane.showMessageDialog(this, "Veuillez charger une image.", "Aucune image", JOptionPane.WARNING_MESSAGE); return; }
        CImage imageSource = (imageNG != null) ? imageNG : imageRGB;

        try {
            System.out.println("Application Transformation: " + operationName + "...");
            int[][] inputMatrix = ImageUtils.imageToGrayMatrix(imageSource);
            if (inputMatrix == null) throw new RuntimeException("Erreur conversion image.");

            // Afficher l'histogramme AVANT si demandé
            if (showHistograms) {
                int[] histoAvant = Histogramme.Histogramme256(inputMatrix);
                if (histoAvant != null) displayHistogram(histoAvant, "Histogramme AVANT " + operationName);
            }

            // Créer la courbe tonale (LUT)
            int[] lut = lutCreator.apply(inputMatrix);
            if (lut == null) throw new RuntimeException("Erreur lors de la création de la courbe tonale.");

            // Appliquer le rehaussement
            int[][] resultMatrix = Histogramme.rehaussement(inputMatrix, lut);
            if (resultMatrix == null) throw new RuntimeException("Erreur pendant l'opération de rehaussement.");

            // Afficher l'histogramme APRÈS si demandé
            if (showHistograms) {
                int[] histoApres = Histogramme.Histogramme256(resultMatrix);
                if (histoApres != null) displayHistogram(histoApres, "Histogramme APRÈS " + operationName);
            }

            // Convertir et mettre à jour l'affichage
            CImageNG resultCImage = ImageUtils.matrixToCImageNG(resultMatrix);
            if (resultCImage == null) throw new RuntimeException("Erreur conversion résultat.");

            updateImageDisplay(resultCImage);
            System.out.println("Transformation " + operationName + " appliquée.");

        } catch (Exception ex) {
            handleProcessingError(operationName, ex);
        }
    }

// --- NOUVELLE MÉTHODE HELPER pour afficher un histogramme ---

    /**
     * Affiche un histogramme dans une fenêtre JFreeChart.
     * @param histo Le tableau de l'histogramme (int[256]).
     * @param title Le titre de la fenêtre du graphique.
     */
    private void displayHistogram(int[] histo, String title) {
        if (histo == null || histo.length != 256) {
            System.err.println("displayHistogram: Données d'histogramme invalides.");
            return;
        }

        // Création du dataset
        XYSeries series = new XYSeries("Histogramme");
        for (int i = 0; i < 256; i++) {
            series.add(i, histo[i]);
        }
        XYSeriesCollection dataset = new XYSeriesCollection(series);

        // Creation du chart (utiliser createXYBarChart pour mieux voir les barres)
        JFreeChart chart = ChartFactory.createXYBarChart(
                title,                     // Titre Chart
                "Niveau de Gris",          // Label Axe X
                false,                     // Date Axis? Non
                "Nombre de Pixels",        // Label Axe Y
                dataset,                   // Données
                PlotOrientation.VERTICAL,
                false,                     // Légende? Non
                true,                      // Tooltips? Oui
                false                      // URLs? Non
        );

        // Ajuster l'axe X
        XYPlot plot = chart.getXYPlot();
        ValueAxis domainAxis = plot.getDomainAxis();
        domainAxis.setRange(-0.5, 255.5); // Pour mieux centrer les barres
        domainAxis.setVerticalTickLabels(false); // Peut aider si les labels se chevauchent

        // Afficher dans une frame
        ChartFrame frame = new ChartFrame(title, chart);
        frame.pack();
        frame.setLocationRelativeTo(this); // Centrer par rapport à la fenêtre principale
        frame.setVisible(true);
    }
    // --- Méthodes pour gérer les filtres globaux (pour éviter la duplication) ---

    /** Gère l'application d'un filtre Idéal (Passe-Bas ou Passe-Haut) */
    private void handleIdealFilter(boolean isLowPass) {
        String filterType = isLowPass ? "Passe-Bas Idéal" : "Passe-Haut Idéal";
        if (imageNG == null && imageRGB == null) { JOptionPane.showMessageDialog(this, "Veuillez charger une image.", "Aucune image", JOptionPane.WARNING_MESSAGE); return; }
        CImage imageSource = (imageNG != null) ? imageNG : imageRGB;

        String freqStr = JOptionPane.showInputDialog(this, "Fréquence de coupure D0:", filterType, JOptionPane.QUESTION_MESSAGE);
        if (freqStr == null || freqStr.trim().isEmpty()) return;

        try {
            int freqCoupure = Integer.parseInt(freqStr.trim());
            if (freqCoupure <= 0) throw new NumberFormatException("Fréquence doit être positive.");

            System.out.println("Application Filtre " + filterType + "...");
            int[][] inputMatrix = ImageUtils.imageToGrayMatrix(imageSource);
            if (inputMatrix == null) throw new RuntimeException("Erreur conversion image.");

            int[][] resultMatrix;
            if (isLowPass) {
                resultMatrix = FiltrageLinaireGlobal.filtrePasseBasIdeal(inputMatrix, freqCoupure);
            } else {
                resultMatrix = FiltrageLinaireGlobal.filtrePasseHautIdeal(inputMatrix, freqCoupure);
            }

            if (resultMatrix == null) throw new RuntimeException("Erreur filtre " + filterType + ".");

            CImageNG resultCImage = ImageUtils.matrixToCImageNG(resultMatrix);
            if (resultCImage == null) throw new RuntimeException("Erreur conversion résultat.");

            updateImageDisplay(resultCImage); // Mettre à jour l'affichage
            System.out.println("Filtre " + filterType + " appliqué.");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Erreur saisie: " + ex.getMessage(), "Erreur Paramètre", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            handleProcessingError(filterType, ex);
        }
    }

    /** Gère l'application d'un filtre Butterworth (Passe-Bas ou Passe-Haut) */
    private void handleButterworthFilter(boolean isLowPass) {
        String filterType = isLowPass ? "Passe-Bas Butterworth" : "Passe-Haut Butterworth";
        if (imageNG == null && imageRGB == null) { JOptionPane.showMessageDialog(this, "Veuillez charger une image.", "Aucune image", JOptionPane.WARNING_MESSAGE); return; }
        CImage imageSource = (imageNG != null) ? imageNG : imageRGB;

        // Utiliser un JPanel pour demander deux valeurs (Fréquence et Ordre)
        JTextField freqField = new JTextField(5);
        JTextField orderField = new JTextField(5);

        JPanel myPanel = new JPanel();
        myPanel.setLayout(new GridLayout(2, 2, 5, 5)); // Layout simple
        myPanel.add(new JLabel("Fréquence Coupure D0:"));
        myPanel.add(freqField);
        myPanel.add(new JLabel("Ordre du filtre n:"));
        myPanel.add(orderField);

        int result = JOptionPane.showConfirmDialog(this, myPanel,
                filterType, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int freqCoupure = Integer.parseInt(freqField.getText().trim());
                int ordre = Integer.parseInt(orderField.getText().trim());

                if (freqCoupure <= 0) throw new NumberFormatException("Fréquence doit être positive.");
                if (ordre <= 0) throw new NumberFormatException("Ordre doit être positif."); // Ou >= 1 selon la théorie

                System.out.println("Application Filtre " + filterType + "...");
                int[][] inputMatrix = ImageUtils.imageToGrayMatrix(imageSource);
                if (inputMatrix == null) throw new RuntimeException("Erreur conversion image.");

                int[][] resultMatrix;
                if (isLowPass) {
                    resultMatrix = FiltrageLinaireGlobal.filtrePasseBasButterworth(inputMatrix, freqCoupure, ordre);
                } else {
                    resultMatrix = FiltrageLinaireGlobal.filtrePasseHautButterworth(inputMatrix, freqCoupure, ordre);
                }

                if (resultMatrix == null) throw new RuntimeException("Erreur filtre " + filterType + ".");

                CImageNG resultCImage = ImageUtils.matrixToCImageNG(resultMatrix);
                if (resultCImage == null) throw new RuntimeException("Erreur conversion résultat.");

                updateImageDisplay(resultCImage); // Mettre à jour l'affichage
                System.out.println("Filtre " + filterType + " appliqué.");

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Erreur saisie: Vérifiez que la fréquence et l'ordre sont des entiers valides.\n" + ex.getMessage(), "Erreur Paramètres", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                handleProcessingError(filterType, ex);
            }
        }
    }

    /** Gère l'application d'une opération morphologique élémentaire */
    private void handleMorphoOperation(MorphoOperation operation) {
        String operationName = operation.toString(); // Nom pour les messages
        if (imageNG == null && imageRGB == null) { JOptionPane.showMessageDialog(this, "Veuillez charger une image.", "Aucune image", JOptionPane.WARNING_MESSAGE); return; }
        CImage imageSource = (imageNG != null) ? imageNG : imageRGB;

        String tailleStr = JOptionPane.showInputDialog(this, "Taille de l'élément structurant carré (entier impair):", operationName, JOptionPane.QUESTION_MESSAGE);
        if (tailleStr == null || tailleStr.trim().isEmpty()) return;

        try {
            int tailleMasque = Integer.parseInt(tailleStr.trim());
            if (tailleMasque <= 0 || tailleMasque % 2 == 0) throw new NumberFormatException("Taille invalide (doit être entier positif impair).");

            System.out.println("Application Opération Morpho: " + operationName + "...");
            int[][] inputMatrix = ImageUtils.imageToGrayMatrix(imageSource);
            if (inputMatrix == null) throw new RuntimeException("Erreur conversion image.");

            int[][] resultMatrix = null;
            // Sélectionner l'opération à appeler en fonction de l'enum
            switch (operation) {
                case EROSION:
                    resultMatrix = ImageProcessing.NonLineaire.MorphoElementaire.erosion(inputMatrix, tailleMasque);
                    break;
                case DILATATION:
                    resultMatrix = ImageProcessing.NonLineaire.MorphoElementaire.dilatation(inputMatrix, tailleMasque);
                    break;
                case OUVERTURE:
                    resultMatrix = ImageProcessing.NonLineaire.MorphoElementaire.ouverture(inputMatrix, tailleMasque);
                    break;
                case FERMETURE:
                    resultMatrix = ImageProcessing.NonLineaire.MorphoElementaire.fermeture(inputMatrix, tailleMasque);
                    break;
            }

            if (resultMatrix == null) throw new RuntimeException("Erreur pendant l'opération " + operationName + ".");

            CImageNG resultCImage = ImageUtils.matrixToCImageNG(resultMatrix);
            if (resultCImage == null) throw new RuntimeException("Erreur conversion résultat.");

            updateImageDisplay(resultCImage); // Mettre à jour l'affichage
            System.out.println("Opération " + operationName + " appliquée.");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Erreur saisie: " + ex.getMessage(), "Erreur Paramètre", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            handleProcessingError(operationName, ex);
        }
    }

    /** Gère l'application d'une opération géodésique (Dilatation itérative ou Reconstruction) */
    private void handleGeodesicOperation(boolean isIterativeDilatation) {
        String operationName = isIterativeDilatation ? "Dilatation Géodésique" : "Reconstruction Géodésique";

        // 1. Vérifier l'image principale (MASQUE)
        if (imageNG == null && imageRGB == null) { JOptionPane.showMessageDialog(this, "Veuillez charger une image principale (Masque).", "Aucune image principale", JOptionPane.WARNING_MESSAGE); return; }
        CImage imageMasqueSource = (imageNG != null) ? imageNG : imageRGB;

        // 2. Vérifier l'image secondaire (MARQUEUR)
        if (secondaryImageMatrix == null) {
            JOptionPane.showMessageDialog(this, "Veuillez charger une image secondaire (Marqueur) via le menu 'Image'.", "Aucune image marqueur", JOptionPane.WARNING_MESSAGE); return;
        }

        int nbIter = -1; // Sera utilisé seulement si isIterativeDilatation est true

        // 3. Demander le nombre d'itérations si nécessaire
        if (isIterativeDilatation) {
            String iterStr = JOptionPane.showInputDialog(this, "Nombre d'itérations:", operationName, JOptionPane.QUESTION_MESSAGE);
            if (iterStr == null || iterStr.trim().isEmpty()) return; // Annulation
            try {
                nbIter = Integer.parseInt(iterStr.trim());
                if (nbIter < 1) throw new NumberFormatException("Le nombre d'itérations doit être >= 1.");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Erreur saisie: " + ex.getMessage(), "Erreur Paramètre", JOptionPane.ERROR_MESSAGE);
                return; // Arrêter si le paramètre est invalide
            }
            if (nbIter < 1) return; // Quitter si invalide
        }

// 4. Lancer le traitement
        try {
            System.out.println("Application Opération: " + operationName + "...");

            // Préparer le MASQUE à partir de l'image principale
            int[][] masque = ImageUtils.imageToGrayMatrix(imageMasqueSource);
            if (masque == null) throw new RuntimeException("Erreur conversion image principale (Masque).");

            // Utiliser l'image secondaire comme MARQUEUR
            int[][] marqueur = this.secondaryImageMatrix; // Déjà en int[][]

            // ---> VÉRIFIER LES DIMENSIONS <---
            if (marqueur.length != masque.length || marqueur[0].length != masque[0].length) {
                throw new RuntimeException("Les dimensions de l'image principale (Masque) et secondaire (Marqueur) doivent être identiques !");
            }

            // Optionnel mais recommandé: Vérifier que marqueur <= masque point par point
            // (Sinon le résultat peut être étrange)
            // for (int y=0; y<masque.length; y++) { for (int x=0; x<masque[0].length; x++) { if (marqueur[y][x] > masque[y][x]) { throw new RuntimeException("Condition Marqueur <= Masque non respectée !");}}}


            int[][] resultMatrix = null;
            if (isIterativeDilatation) {
                resultMatrix = ImageProcessing.NonLineaire.MorphoComplexe.dilatationGeodesique(marqueur, masque, nbIter);
            } else {
                resultMatrix = ImageProcessing.NonLineaire.MorphoComplexe.reconstructionGeodesique(marqueur, masque);
            }

            if (resultMatrix == null) throw new RuntimeException("Erreur pendant l'opération " + operationName + ".");

            CImageNG resultCImage = ImageUtils.matrixToCImageNG(resultMatrix);
            if (resultCImage == null) throw new RuntimeException("Erreur conversion résultat.");

            updateImageDisplay(resultCImage); // Mettre à jour l'affichage avec le résultat
            System.out.println("Opération " + operationName + " appliquée.");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Erreur saisie: " + ex.getMessage(), "Erreur Paramètre", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            handleProcessingError(operationName, ex);
        }
    }
    /**
     * Gère l'appel aux fonctions de calcul de gradient (Prewitt ou Sobel).
     * @param operationName Nom pour les logs/messages.
     * @param direction Direction du gradient (1=H, 2=V).
     * @param isPrewitt True pour Prewitt, False pour Sobel.
     */
    private void handleGradient(String operationName, int direction, boolean isPrewitt) {
        if (imageNG == null && imageRGB == null) { JOptionPane.showMessageDialog(this, "Veuillez charger une image.", "Aucune image", JOptionPane.WARNING_MESSAGE); return; }
        CImage imageSource = (imageNG != null) ? imageNG : imageRGB;

        try {
            System.out.println("Application Opérateur: " + operationName + "...");
            int[][] inputMatrix = ImageUtils.imageToGrayMatrix(imageSource);
            if (inputMatrix == null) throw new RuntimeException("Erreur conversion image.");

            int[][] resultMatrix;
            if (isPrewitt) {
                resultMatrix = ContoursLineaire.gradientPrewitt(inputMatrix, direction);
            } else {
                resultMatrix = ContoursLineaire.gradientSobel(inputMatrix, direction);
            }

            if (resultMatrix == null) throw new RuntimeException("Erreur pendant l'opération " + operationName + ".");

            CImageNG resultCImage = ImageUtils.matrixToCImageNG(resultMatrix);
            if (resultCImage == null) throw new RuntimeException("Erreur conversion résultat.");

            updateImageDisplay(resultCImage);
            System.out.println("Opération " + operationName + " appliquée.");

        } catch (Exception ex) {
            handleProcessingError(operationName, ex);
        }
    }

    /**
     * Gère l'appel aux fonctions de calcul de Laplacien (4 ou 8).
     * @param operationName Nom pour les logs/messages.
     * @param isLaplacien4 True pour Laplacien 4, False pour Laplacien 8.
     */
    private void handleLaplacien(String operationName, boolean isLaplacien4) {
        if (imageNG == null && imageRGB == null) { JOptionPane.showMessageDialog(this, "Veuillez charger une image.", "Aucune image", JOptionPane.WARNING_MESSAGE); return; }
        CImage imageSource = (imageNG != null) ? imageNG : imageRGB;

        try {
            System.out.println("Application Opérateur: " + operationName + "...");
            int[][] inputMatrix = ImageUtils.imageToGrayMatrix(imageSource);
            if (inputMatrix == null) throw new RuntimeException("Erreur conversion image.");

            int[][] resultMatrix;
            if (isLaplacien4) {
                resultMatrix = ContoursLineaire.laplacien4(inputMatrix);
            } else {
                resultMatrix = ContoursLineaire.laplacien8(inputMatrix);
            }

            if (resultMatrix == null) throw new RuntimeException("Erreur pendant l'opération " + operationName + ".");

            CImageNG resultCImage = ImageUtils.matrixToCImageNG(resultMatrix);
            if (resultCImage == null) throw new RuntimeException("Erreur conversion résultat.");

            updateImageDisplay(resultCImage);
            System.out.println("Opération " + operationName + " appliquée.");

        } catch (Exception ex) {
            handleProcessingError(operationName, ex);
        }
    }
    /**
     * Gère l'appel aux fonctions de calcul de contours non-linéaires.
     * @param operationName Nom pour les logs/messages.
     * @param type Identifiant de l'opération (1:GradErosion, 2:GradDilat, 3:GradBeucher, 4:LaplacienNL).
     */
    private void handleContourNonLineaire(String operationName, int type) {
        if (imageNG == null && imageRGB == null) { JOptionPane.showMessageDialog(this, "Veuillez charger une image.", "Aucune image", JOptionPane.WARNING_MESSAGE); return; }
        CImage imageSource = (imageNG != null) ? imageNG : imageRGB;

        try {
            System.out.println("Application Opérateur: " + operationName + "...");
            int[][] inputMatrix = ImageUtils.imageToGrayMatrix(imageSource);
            if (inputMatrix == null) throw new RuntimeException("Erreur conversion image.");

            int[][] resultMatrix = null;
            switch (type) {
                case 1: resultMatrix = ContoursNonLineaire.gradientErosion(inputMatrix); break;
                case 2: resultMatrix = ContoursNonLineaire.gradientDilatation(inputMatrix); break;
                case 3: resultMatrix = ContoursNonLineaire.gradientBeucher(inputMatrix); break;
                case 4: resultMatrix = ContoursNonLineaire.laplacienNonLineaire(inputMatrix); break;
                default: throw new IllegalArgumentException("Type d'opération non linéaire inconnu: " + type);
            }

            if (resultMatrix == null) throw new RuntimeException("Erreur pendant l'opération " + operationName + ".");

            CImageNG resultCImage = ImageUtils.matrixToCImageNG(resultMatrix);
            if (resultCImage == null) throw new RuntimeException("Erreur conversion résultat.");

            updateImageDisplay(resultCImage);
            System.out.println("Opération " + operationName + " appliquée.");

        } catch (Exception ex) {
            handleProcessingError(operationName, ex);
        }
    }

    /** Gère l'appel au seuillage simple. */
    private void handleSeuillageSimple() {
        String operationName = "Seuillage Simple";
        if (imageNG == null && imageRGB == null) { JOptionPane.showMessageDialog(this, "Veuillez charger une image.", "Aucune image", JOptionPane.WARNING_MESSAGE); return; }
        CImage imageSource = (imageNG != null) ? imageNG : imageRGB;

        String seuilStr = JOptionPane.showInputDialog(this, "Entrez la valeur de seuil (0-255):", operationName, JOptionPane.QUESTION_MESSAGE);
        if (seuilStr == null || seuilStr.trim().isEmpty()) return;

        try {
            int seuil = Integer.parseInt(seuilStr.trim());
            // Pas besoin de validation stricte 0-255 ici, la méthode seuillageSimple peut gérer

            System.out.println("Application " + operationName + "...");
            int[][] inputMatrix = ImageUtils.imageToGrayMatrix(imageSource);
            if (inputMatrix == null) throw new RuntimeException("Erreur conversion image.");

            int[][] resultMatrix = Seuillage.seuillageSimple(inputMatrix, seuil);

            if (resultMatrix == null) throw new RuntimeException("Erreur pendant l'opération " + operationName + ".");

            CImageNG resultCImage = ImageUtils.matrixToCImageNG(resultMatrix);
            if (resultCImage == null) throw new RuntimeException("Erreur conversion résultat.");

            updateImageDisplay(resultCImage);
            System.out.println("Opération " + operationName + " appliquée.");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Erreur saisie: Le seuil doit être un entier.", "Erreur Paramètre", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            handleProcessingError(operationName, ex);
        }
    }


    /** Gère l'appel au seuillage double. */
    private void handleSeuillageDouble() {
        String operationName = "Seuillage Double";
        if (imageNG == null && imageRGB == null) { JOptionPane.showMessageDialog(this, "Veuillez charger une image.", "Aucune image", JOptionPane.WARNING_MESSAGE); return; }
        CImage imageSource = (imageNG != null) ? imageNG : imageRGB;

        // Dialogue pour Seuil1 et Seuil2
        JTextField seuil1Field = new JTextField("85", 5);  // Valeurs par défaut exemple
        JTextField seuil2Field = new JTextField("170", 5);
        JPanel panelSeuils = new JPanel(new GridLayout(2, 2, 5, 5));
        panelSeuils.add(new JLabel("Seuil 1 (inférieur):"));
        panelSeuils.add(seuil1Field);
        panelSeuils.add(new JLabel("Seuil 2 (supérieur):"));
        panelSeuils.add(seuil2Field);

        int result = JOptionPane.showConfirmDialog(this, panelSeuils, operationName, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int seuil1 = Integer.parseInt(seuil1Field.getText().trim());
                int seuil2 = Integer.parseInt(seuil2Field.getText().trim());

                if (seuil1 > seuil2) {
                    throw new NumberFormatException("Seuil 1 doit être <= Seuil 2.");
                }

                System.out.println("Application " + operationName + "...");
                int[][] inputMatrix = ImageUtils.imageToGrayMatrix(imageSource);
                if (inputMatrix == null) throw new RuntimeException("Erreur conversion image.");

                int[][] resultMatrix = Seuillage.seuillageDouble(inputMatrix, seuil1, seuil2);

                if (resultMatrix == null) throw new RuntimeException("Erreur pendant l'opération " + operationName + ".");

                CImageNG resultCImage = ImageUtils.matrixToCImageNG(resultMatrix);
                if (resultCImage == null) throw new RuntimeException("Erreur conversion résultat.");

                updateImageDisplay(resultCImage);
                System.out.println("Opération " + operationName + " appliquée.");

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Erreur saisie: " + ex.getMessage(), "Erreur Paramètres", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                handleProcessingError(operationName, ex);
            }
        }
    }

    /** Gère l'appel au seuillage automatique (Otsu). */
    private void handleSeuillageAutomatique() {
        String operationName = "Seuillage Automatique (Otsu)";
        if (imageNG == null && imageRGB == null) { JOptionPane.showMessageDialog(this, "Veuillez charger une image.", "Aucune image", JOptionPane.WARNING_MESSAGE); return; }
        CImage imageSource = (imageNG != null) ? imageNG : imageRGB;

        try {
            System.out.println("Application " + operationName + "...");
            int[][] inputMatrix = ImageUtils.imageToGrayMatrix(imageSource);
            if (inputMatrix == null) throw new RuntimeException("Erreur conversion image.");

            int[][] resultMatrix = Seuillage.seuillageAutomatique(inputMatrix);

            if (resultMatrix == null) throw new RuntimeException("Erreur pendant l'opération " + operationName + ".");

            CImageNG resultCImage = ImageUtils.matrixToCImageNG(resultMatrix);
            if (resultCImage == null) throw new RuntimeException("Erreur conversion résultat.");

            updateImageDisplay(resultCImage);
            System.out.println("Opération " + operationName + " appliquée.");

        } catch (Exception ex) {
            handleProcessingError(operationName, ex);
        }
    }

    /** Méthode utilitaire pour afficher les erreurs de traitement */
    private void handleProcessingError(String filterName, Exception ex) {
        System.err.println("Erreur filtre [" + filterName + "]: " + ex.getMessage());
        ex.printStackTrace(); // Pour le débogage
        JOptionPane.showMessageDialog(this,
                "Erreur lors de l'application du filtre [" + filterName + "]:\n" + ex.getMessage(),
                "Erreur Traitement", JOptionPane.ERROR_MESSAGE);

    }
    private void jMenuItemOuvrirNGActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser choix = new JFileChooser();
        File fichier;

        choix.setCurrentDirectory(new File ("."));
        if (choix.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
        {
            fichier = choix.getSelectedFile();
            originalImageMatrix = null; // Réinitialiser au cas où le chargement échoue
            if (fichier != null)
            {
                try
                {
                    imageNG = new CImageNG(fichier);
                    // ---> STOCKER L'ORIGINAL <---
                    originalImageMatrix = ImageUtils.imageToGrayMatrix(imageNG);
                    if (originalImageMatrix == null) {
                        System.err.println("Erreur conversion image NG originale lors de l'ouverture.");
                        // Pas besoin de bloquer, mais originalImageMatrix restera null
                    }
                    observer.setCImage(imageNG);
                    imageRGB = null;
                    activeMenusNG(); // Active les menus standards pour NG

                }
                catch (IOException ex) { // Catcher les deux
                    System.err.println("Erreur I/O ou CImageNG lors de l'ouverture: " + ex.getMessage());
                    // Assurer que les images sont nulles si le chargement échoue
                    imageNG = null;
                    imageRGB = null;
                    originalImageMatrix = null;
                    // Peut-être désactiver tous les menus ici ?
                }
            }
            updateRevenirOriginalMenuState(); // Mettre à jour l'état du menu "Revenir" DANS TOUS LES CAS
        }
    }

    private void jMenuItemOuvrirRGBActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser choix = new JFileChooser();
        File fichier;

        choix.setCurrentDirectory(new File ("."));
        if (choix.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
        {
            fichier = choix.getSelectedFile();
            originalImageMatrix = null; // Réinitialiser
            if (fichier != null)
            {
                try
                {
                    imageRGB = new CImageRGB(fichier);
                    // ---> STOCKER L'ORIGINAL (en NG) <---
                    originalImageMatrix = ImageUtils.imageToGrayMatrix(imageRGB);
                    if (originalImageMatrix == null) {
                        System.err.println("Erreur conversion image RGB originale lors de l'ouverture.");
                    }
                    observer.setCImage(imageRGB); // Affiche l'original RGB
                    imageNG = null;
                    activeMenusRGB(); // Active les menus standards pour RGB

                }
                catch (IOException ex) { // Catcher les deux
                    System.err.println("Erreur I/O ou CImageRGB lors de l'ouverture: " + ex.getMessage());
                    imageNG = null;
                    imageRGB = null;
                    originalImageMatrix = null;
                }
            }
            updateRevenirOriginalMenuState(); // Mettre à jour l'état du menu "Revenir" DANS TOUS LES CAS
        }

    }
// Idem pour "Nouvelle Image NG/RGB" si tu veux pouvoir revenir à l'image vide initiale.
    /** Méthode centralisée pour mettre à jour l'image affichée */
    private void updateImageDisplay(CImageNG newImage) {
        if (newImage != null) {
            imageNG = newImage;
            imageRGB = null; // Le résultat du traitement est NG
            observer.setCImage(imageNG);
            activeMenusNG(); // Activer les menus pour NG
        } else {
            System.err.println("Tentative de mise à jour avec une image null.");
            // Optionnel: Afficher un message d'erreur à l'utilisateur
        }
    }
    /** Active ou désactive le menu "Revenir à l'original"
     *  en fonction de la présence d'une image originale stockée
     *  et si l'image actuelle est différente (simplifié). */
    /** Active ou désactive le menu "Revenir à l'original"
     *  en fonction de la présence d'une image originale stockée. */
    private void updateRevenirOriginalMenuState() {
        // PAS DE DÉCLARATION DE VARIABLE LOCALE ICI

        // On vérifie si la variable membre itemRevenirOriginal (le JMenuItem) a été initialisée
        // et si une matrice originale est actuellement stockée.
        if (this.itemRevenirOriginal != null) { // Utiliser this. pour être explicite (ou juste itemRevenirOriginal)

            boolean canRevert = (this.originalImageMatrix != null); // Le bouton est actif si un original existe

            // Logique simplifiée: Toujours activer si un original existe.
            // L'utilisateur peut cliquer même si l'image actuelle est déjà l'original,
            // cela ne fera que réappliquer l'original sur lui-même.
            this.itemRevenirOriginal.setEnabled(canRevert);

            /* // Logique plus complexe (optionnelle et peut-être moins robuste)
               // pour désactiver si l'image actuelle *est* l'original:
               boolean isCurrentlyOriginal = false;
               if (imageNG != null && originalImageMatrix != null) {
                   // Comparer les matrices serait la seule façon fiable, mais potentiellement lourd.
                   // Comparer les objets CImageNG ou BufferedImage n'est pas fiable.
                   // On pourrait supposer que si imageRGB est null et imageNG existe,
                   // on n'est plus sur l'original si celui-ci venait d'une image RGB.
                   // Pour simplifier, on laisse actif dès qu'un original existe.
               }
               this.itemRevenirOriginal.setEnabled(canRevert && !isCurrentlyOriginal);
            */
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroupDessiner = new javax.swing.ButtonGroup();
        jScrollPane = new javax.swing.JScrollPane();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenuImage = new javax.swing.JMenu();
        jMenuNouvelle = new javax.swing.JMenu();
        jMenuItemNouvelleRGB = new javax.swing.JMenuItem();
        jMenuItemNouvelleNG = new javax.swing.JMenuItem();
        jMenuOuvrir = new javax.swing.JMenu();
        jMenuItemOuvrirRGB = new javax.swing.JMenuItem();
        jMenuItemOuvrirNG = new javax.swing.JMenuItem();
        jMenuItemEnregistrerSous = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        jMenuQuitter = new javax.swing.JMenuItem();
        jMenuDessiner = new javax.swing.JMenu();
        jMenuItemCouleurPinceau = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        jCheckBoxMenuItemDessinerPixel = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemDessinerLigne = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemDessinerRectangle = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemDessinerRectanglePlein = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemDessinerCercle = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemDessinerCerclePlein = new javax.swing.JCheckBoxMenuItem();
        jMenuFourier = new javax.swing.JMenu();
        jMenuFourierAfficher = new javax.swing.JMenu();
        jMenuItemFourierAfficherModule = new javax.swing.JMenuItem();
        jMenuItemFourierAfficherPhase = new javax.swing.JMenuItem();
        jMenuItemFourierAfficherPartieReelle = new javax.swing.JMenuItem();
        jMenuItemFourierAfficherPartieImaginaire = new javax.swing.JMenuItem();
        jMenuHistogramme = new javax.swing.JMenu();
        jMenuHistogrammeAfficher = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Isil Image Processing");

        jMenuImage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Icones/Net 13_p1.jpg"))); // NOI18N
        jMenuImage.setText("Image");

        jMenuNouvelle.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Icones/File 65_p3.jpg"))); // NOI18N
        jMenuNouvelle.setText("Nouvelle");

        jMenuItemNouvelleRGB.setText("Image RGB");
        jMenuItemNouvelleRGB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemNouvelleRGBActionPerformed(evt);
            }
        });
        jMenuNouvelle.add(jMenuItemNouvelleRGB);

        jMenuItemNouvelleNG.setText("Image NG");
        jMenuItemNouvelleNG.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemNouvelleNGActionPerformed(evt);
            }
        });
        jMenuNouvelle.add(jMenuItemNouvelleNG);

        jMenuImage.add(jMenuNouvelle);

        jMenuOuvrir.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Icones/Folder 036_p3.jpg"))); // NOI18N
        jMenuOuvrir.setText("Ouvrir");

        jMenuItemOuvrirRGB.setText("Image RGB");
        jMenuItemOuvrirRGB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOuvrirRGBActionPerformed(evt);
            }
        });
        jMenuOuvrir.add(jMenuItemOuvrirRGB);

        jMenuItemOuvrirNG.setText("Image NG");
        jMenuItemOuvrirNG.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOuvrirNGActionPerformed(evt);
            }
        });
        jMenuOuvrir.add(jMenuItemOuvrirNG);

        jMenuImage.add(jMenuOuvrir);

        jMenuItemEnregistrerSous.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Icones/DD 27_p3.jpg"))); // NOI18N
        jMenuItemEnregistrerSous.setText("Enregistrer sous...");
        jMenuItemEnregistrerSous.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemEnregistrerSousActionPerformed(evt);
            }
        });
        jMenuImage.add(jMenuItemEnregistrerSous);
        jMenuImage.add(jSeparator1);

        jMenuQuitter.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Icones/CP 59_p3.jpg"))); // NOI18N
        jMenuQuitter.setText("Quitter");
        jMenuQuitter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuQuitterActionPerformed(evt);
            }
        });
        jMenuImage.add(jMenuQuitter);

        jMenuBar1.add(jMenuImage);

        jMenuDessiner.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Icones/Display 28_p1.jpg"))); // NOI18N
        jMenuDessiner.setText("Dessiner");

        jMenuItemCouleurPinceau.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Icones/Display 14_p3.jpg"))); // NOI18N
        jMenuItemCouleurPinceau.setText("Couleur");
        jMenuItemCouleurPinceau.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCouleurPinceauActionPerformed(evt);
            }
        });
        jMenuDessiner.add(jMenuItemCouleurPinceau);
        jMenuDessiner.add(jSeparator2);

        jCheckBoxMenuItemDessinerPixel.setText("Pixel");
        jCheckBoxMenuItemDessinerPixel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemDessinerPixelActionPerformed(evt);
            }
        });
        jMenuDessiner.add(jCheckBoxMenuItemDessinerPixel);

        jCheckBoxMenuItemDessinerLigne.setText("Ligne");
        jCheckBoxMenuItemDessinerLigne.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemDessinerLigneActionPerformed(evt);
            }
        });
        jMenuDessiner.add(jCheckBoxMenuItemDessinerLigne);

        jCheckBoxMenuItemDessinerRectangle.setText("Rectangle");
        jCheckBoxMenuItemDessinerRectangle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemDessinerRectangleActionPerformed(evt);
            }
        });
        jMenuDessiner.add(jCheckBoxMenuItemDessinerRectangle);

        jCheckBoxMenuItemDessinerRectanglePlein.setText("Rectangle plein");
        jCheckBoxMenuItemDessinerRectanglePlein.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemDessinerRectanglePleinActionPerformed(evt);
            }
        });
        jMenuDessiner.add(jCheckBoxMenuItemDessinerRectanglePlein);

        jCheckBoxMenuItemDessinerCercle.setText("Cercle");
        jCheckBoxMenuItemDessinerCercle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemDessinerCercleActionPerformed(evt);
            }
        });
        jMenuDessiner.add(jCheckBoxMenuItemDessinerCercle);

        jCheckBoxMenuItemDessinerCerclePlein.setText("Cercle plein");
        jCheckBoxMenuItemDessinerCerclePlein.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemDessinerCerclePleinActionPerformed(evt);
            }
        });
        jMenuDessiner.add(jCheckBoxMenuItemDessinerCerclePlein);

        jMenuBar1.add(jMenuDessiner);

        jMenuFourier.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Icones/CP 51_p1.jpg"))); // NOI18N
        jMenuFourier.setText("Fourier");

        jMenuFourierAfficher.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Icones/CP 51_p3.jpg"))); // NOI18N
        jMenuFourierAfficher.setText("Afficher");

        jMenuItemFourierAfficherModule.setText("Module");
        jMenuItemFourierAfficherModule.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemFourierAfficherModuleActionPerformed(evt);
            }
        });
        jMenuFourierAfficher.add(jMenuItemFourierAfficherModule);

        jMenuItemFourierAfficherPhase.setText("Phase");
        jMenuItemFourierAfficherPhase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemFourierAfficherPhaseActionPerformed(evt);
            }
        });
        jMenuFourierAfficher.add(jMenuItemFourierAfficherPhase);

        jMenuItemFourierAfficherPartieReelle.setText("Partie Reelle");
        jMenuItemFourierAfficherPartieReelle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemFourierAfficherPartieReelleActionPerformed(evt);
            }
        });
        jMenuFourierAfficher.add(jMenuItemFourierAfficherPartieReelle);

        jMenuItemFourierAfficherPartieImaginaire.setText("Partie Imaginaire");
        jMenuItemFourierAfficherPartieImaginaire.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemFourierAfficherPartieImaginaireActionPerformed(evt);
            }
        });
        jMenuFourierAfficher.add(jMenuItemFourierAfficherPartieImaginaire);

        jMenuFourier.add(jMenuFourierAfficher);

        jMenuBar1.add(jMenuFourier);

        jMenuHistogramme.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Icones/report_48_hot.jpg"))); // NOI18N
        jMenuHistogramme.setText("Histogramme");

        jMenuHistogrammeAfficher.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Icones/report_32_hot.jpg"))); // NOI18N
        jMenuHistogrammeAfficher.setText("Afficher");
        jMenuHistogrammeAfficher.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuHistogrammeAfficherActionPerformed(evt);
            }
        });
        jMenuHistogramme.add(jMenuHistogrammeAfficher);

        jMenuBar1.add(jMenuHistogramme);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 464, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE)
                .addContainerGap())
        );

        setSize(new java.awt.Dimension(500, 400));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuHistogrammeAfficherActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuHistogrammeAfficherActionPerformed
        int histo[];
        try 
        {
            int f_int[][] = imageNG.getMatrice();
            histo = Histogramme.Histogramme256(f_int);
        } 
        catch (CImageNGException ex) 
        {
            System.out.println("Erreur CImageNG : " + ex.getMessage());
            return;
        }
        
        // Cr�ation du dataset
        XYSeries serie = new XYSeries("Histo");
        for(int i=0 ; i<256 ; i++) serie.add(i,histo[i]);
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(serie);
        
        // Creation du chart
        JFreeChart chart = ChartFactory.createHistogram("Histogramme","Niveaux de gris","Nombre de pixels",dataset,PlotOrientation.VERTICAL,false,false,false);

        XYPlot plot = (XYPlot)chart.getXYPlot();
        ValueAxis axeX = plot.getDomainAxis();
        axeX.setRange(0,255);
        plot.setDomainAxis(axeX);
        
        // creation d'une frame
        ChartFrame frame = new ChartFrame("Histogramme de l'image",chart);
        frame.pack();
        frame.setVisible(true);
    }//GEN-LAST:event_jMenuHistogrammeAfficherActionPerformed

    private void activeMenusNG()
    {
        jMenuDessiner.setEnabled(true);
        jMenuFourier.setEnabled(true);
        jMenuHistogramme.setEnabled(true);
        // ---> AJOUTER CETTE LIGNE <---
        if (menuFiltrageLineaire != null) { // Vérif par sécurité
            menuFiltrageLineaire.setEnabled(true); // Activer pour NG
        }
        // ---> AJOUTER CETTE LIGNE <---
        if (menuTraitementNonLineaire != null) {
            menuTraitementNonLineaire.setEnabled(true);
        }
        menuContours.setEnabled(true);
        menuSeuillage.setEnabled(true);
    }

    private void activeMenusRGB() {
        jMenuDessiner.setEnabled(true);
        jMenuFourier.setEnabled(false);
        jMenuHistogramme.setEnabled(false);
        if (menuFiltrageLineaire != null) {
            menuFiltrageLineaire.setEnabled(true);
        }
        // ---> AJOUTER CETTE LIGNE <---
        if (menuTraitementNonLineaire != null) {
            // On l'active aussi car on convertit en NG pour le traitement
            menuTraitementNonLineaire.setEnabled(true);
        }
        menuContours.setEnabled(true);
        menuSeuillage.setEnabled(true);
    }
    
    private void jCheckBoxMenuItemDessinerCerclePleinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemDessinerCerclePleinActionPerformed
        if (!jCheckBoxMenuItemDessinerCerclePlein.isSelected()) observer.setMode(JLabelBeanCImage.INACTIF);
        else
        {
            jCheckBoxMenuItemDessinerPixel.setSelected(false);
            jCheckBoxMenuItemDessinerLigne.setSelected(false);
            jCheckBoxMenuItemDessinerRectangle.setSelected(false);
            jCheckBoxMenuItemDessinerRectanglePlein.setSelected(false);
            jCheckBoxMenuItemDessinerCercle.setSelected(false);
            jCheckBoxMenuItemDessinerCerclePlein.setSelected(true);
            observer.setMode(JLabelBeanCImage.SELECT_CERCLE_FILL);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemDessinerCerclePleinActionPerformed

    private void jCheckBoxMenuItemDessinerCercleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemDessinerCercleActionPerformed
        if (!jCheckBoxMenuItemDessinerCercle.isSelected()) observer.setMode(JLabelBeanCImage.INACTIF);
        else
        {
            jCheckBoxMenuItemDessinerPixel.setSelected(false);
            jCheckBoxMenuItemDessinerLigne.setSelected(false);
            jCheckBoxMenuItemDessinerRectangle.setSelected(false);
            jCheckBoxMenuItemDessinerRectanglePlein.setSelected(false);
            jCheckBoxMenuItemDessinerCercle.setSelected(true);
            jCheckBoxMenuItemDessinerCerclePlein.setSelected(false);
            observer.setMode(JLabelBeanCImage.SELECT_CERCLE);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemDessinerCercleActionPerformed

    private void jMenuItemFourierAfficherPartieImaginaireActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemFourierAfficherPartieImaginaireActionPerformed
        try 
        {
            int f_int[][] = imageNG.getMatrice();
            double f[][] = new double[imageNG.getLargeur()][imageNG.getHauteur()];
            for(int i=0 ; i<imageNG.getLargeur() ; i++)
                for(int j=0 ; j<imageNG.getHauteur() ; j++) f[i][j] = (double)(f_int[i][j]);
            
            System.out.println("Debut Fourier");
            MatriceComplexe fourier = Fourier.Fourier2D(f);
            System.out.println("Fin Fourier");
            fourier = Fourier.decroise(fourier);
            double partieImaginaire[][] = fourier.getPartieImaginaire();
            
            JDialogAfficheMatriceDouble dialog = new JDialogAfficheMatriceDouble(this,true,partieImaginaire,"Fourier : Affichage de la partie imaginaire");
            dialog.setVisible(true);
        } 
        catch (CImageNGException ex) 
        {
            System.out.println("Erreur CImageNG : " + ex.getMessage());
        }
    }//GEN-LAST:event_jMenuItemFourierAfficherPartieImaginaireActionPerformed

    private void jMenuItemFourierAfficherPartieReelleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemFourierAfficherPartieReelleActionPerformed
        try 
        {
            int f_int[][] = imageNG.getMatrice();
            double f[][] = new double[imageNG.getLargeur()][imageNG.getHauteur()];
            for(int i=0 ; i<imageNG.getLargeur() ; i++)
                for(int j=0 ; j<imageNG.getHauteur() ; j++) f[i][j] = (double)(f_int[i][j]);
            
            System.out.println("Debut Fourier");
            MatriceComplexe fourier = Fourier.Fourier2D(f);
            System.out.println("Fin Fourier");
            fourier = Fourier.decroise(fourier);
            double partieReelle[][] = fourier.getPartieReelle();
            
            JDialogAfficheMatriceDouble dialog = new JDialogAfficheMatriceDouble(this,true,partieReelle,"Fourier : Affichage de la partie reelle");
            dialog.setVisible(true);
        } 
        catch (CImageNGException ex) 
        {
            System.out.println("Erreur CImageNG : " + ex.getMessage());
        }

    }//GEN-LAST:event_jMenuItemFourierAfficherPartieReelleActionPerformed

    private void jMenuItemFourierAfficherPhaseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemFourierAfficherPhaseActionPerformed
        try 
        {
            int f_int[][] = imageNG.getMatrice();
            double f[][] = new double[imageNG.getLargeur()][imageNG.getHauteur()];
            for(int i=0 ; i<imageNG.getLargeur() ; i++)
                for(int j=0 ; j<imageNG.getHauteur() ; j++) f[i][j] = (double)(f_int[i][j]);
            
            System.out.println("Debut Fourier");
            MatriceComplexe fourier = Fourier.Fourier2D(f);
            System.out.println("Fin Fourier");
            fourier = Fourier.decroise(fourier);
            double phase[][] = fourier.getPhase();
            
            JDialogAfficheMatriceDouble dialog = new JDialogAfficheMatriceDouble(this,true,phase,"Fourier : Affichage de la phase");
            dialog.setVisible(true);
        } 
        catch (CImageNGException ex) 
        {
            System.out.println("Erreur CImageNG : " + ex.getMessage());
        }

    }//GEN-LAST:event_jMenuItemFourierAfficherPhaseActionPerformed

    private void jMenuItemFourierAfficherModuleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemFourierAfficherModuleActionPerformed
        try 
        {
            int f_int[][] = imageNG.getMatrice();
            double f[][] = new double[imageNG.getLargeur()][imageNG.getHauteur()];
            for(int i=0 ; i<imageNG.getLargeur() ; i++)
                for(int j=0 ; j<imageNG.getHauteur() ; j++) f[i][j] = (double)(f_int[i][j]);
            
            System.out.println("Debut Fourier");
            MatriceComplexe fourier = Fourier.Fourier2D(f);
            System.out.println("Fin Fourier");
            fourier = Fourier.decroise(fourier);
            double module[][] = fourier.getModule();
            
            JDialogAfficheMatriceDouble dialog = new JDialogAfficheMatriceDouble(this,true,module,"Fourier : Affichage du module");
            dialog.setVisible(true);
        } 
        catch (CImageNGException ex) 
        {
            System.out.println("Erreur CImageNG : " + ex.getMessage());
        }
    }//GEN-LAST:event_jMenuItemFourierAfficherModuleActionPerformed

    private void jCheckBoxMenuItemDessinerPixelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemDessinerPixelActionPerformed
        if (!jCheckBoxMenuItemDessinerPixel.isSelected()) observer.setMode(JLabelBeanCImage.INACTIF);
        else
        {
            jCheckBoxMenuItemDessinerPixel.setSelected(true);
            jCheckBoxMenuItemDessinerLigne.setSelected(false);
            jCheckBoxMenuItemDessinerRectangle.setSelected(false);
            jCheckBoxMenuItemDessinerRectanglePlein.setSelected(false);
            jCheckBoxMenuItemDessinerCercle.setSelected(false);
            jCheckBoxMenuItemDessinerCerclePlein.setSelected(false);
            observer.setMode(JLabelBeanCImage.CLIC);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemDessinerPixelActionPerformed

    private void jMenuItemEnregistrerSousActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemEnregistrerSousActionPerformed
        JFileChooser choix = new JFileChooser();
	File fichier;
			
	choix.setCurrentDirectory(new File ("."));
	if (choix.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
	{
            fichier = choix.getSelectedFile();
            if (fichier != null)
            {
                try 
                {
                    if (imageRGB != null) imageRGB.enregistreFormatPNG(fichier);
                    if (imageNG != null) imageNG.enregistreFormatPNG(fichier);
                } 
                catch (IOException ex) 
                {
                    System.err.println("Erreur I/O : " + ex.getMessage());
                }
            }
	}
    }//GEN-LAST:event_jMenuItemEnregistrerSousActionPerformed


    private void jMenuItemNouvelleNGActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemNouvelleNGActionPerformed
        JDialogNouvelleCImageNG dialog = new JDialogNouvelleCImageNG(this,true);
        dialog.setVisible(true);
        imageNG = dialog.getCImageNG();
        observer.setCImage(imageNG);
        imageRGB = null;
        activeMenusNG();
    }//GEN-LAST:event_jMenuItemNouvelleNGActionPerformed

    private void jMenuItemCouleurPinceauActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCouleurPinceauActionPerformed
        if (imageRGB != null)
        {
            Color newC = JColorChooser.showDialog(this,"Couleur du pinceau",couleurPinceauRGB);
            if (newC != null) couleurPinceauRGB = newC;
            observer.setCouleurPinceau(couleurPinceauRGB);
        }
        
        if (imageNG != null)
        {
            JDialogChoixCouleurNG dialog = new JDialogChoixCouleurNG(this,true,couleurPinceauNG);
            dialog.setVisible(true);
            couleurPinceauNG = dialog.getCouleur();
        }
    }//GEN-LAST:event_jMenuItemCouleurPinceauActionPerformed

    private void jCheckBoxMenuItemDessinerRectanglePleinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemDessinerRectanglePleinActionPerformed
        if (!jCheckBoxMenuItemDessinerRectanglePlein.isSelected()) observer.setMode(JLabelBeanCImage.INACTIF);
        else
        {
            jCheckBoxMenuItemDessinerPixel.setSelected(false);
            jCheckBoxMenuItemDessinerLigne.setSelected(false);
            jCheckBoxMenuItemDessinerRectangle.setSelected(false);
            jCheckBoxMenuItemDessinerRectanglePlein.setSelected(true);
            jCheckBoxMenuItemDessinerCercle.setSelected(false);
            jCheckBoxMenuItemDessinerCerclePlein.setSelected(false);
            observer.setMode(JLabelBeanCImage.SELECT_RECT_FILL);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemDessinerRectanglePleinActionPerformed

    private void jCheckBoxMenuItemDessinerRectangleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemDessinerRectangleActionPerformed
        if (!jCheckBoxMenuItemDessinerRectangle.isSelected()) observer.setMode(JLabelBeanCImage.INACTIF);
        else
        {
            jCheckBoxMenuItemDessinerPixel.setSelected(false);
            jCheckBoxMenuItemDessinerLigne.setSelected(false);
            jCheckBoxMenuItemDessinerRectangle.setSelected(true);
            jCheckBoxMenuItemDessinerRectanglePlein.setSelected(false);
            jCheckBoxMenuItemDessinerCercle.setSelected(false);
            jCheckBoxMenuItemDessinerCerclePlein.setSelected(false);
            observer.setMode(JLabelBeanCImage.SELECT_RECT);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemDessinerRectangleActionPerformed

    private void jCheckBoxMenuItemDessinerLigneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemDessinerLigneActionPerformed
        if (!jCheckBoxMenuItemDessinerLigne.isSelected()) observer.setMode(JLabelBeanCImage.INACTIF);
        else
        {
            jCheckBoxMenuItemDessinerPixel.setSelected(false);
            jCheckBoxMenuItemDessinerLigne.setSelected(true);
            jCheckBoxMenuItemDessinerRectangle.setSelected(false);
            jCheckBoxMenuItemDessinerRectanglePlein.setSelected(false);
            jCheckBoxMenuItemDessinerCercle.setSelected(false);
            jCheckBoxMenuItemDessinerCerclePlein.setSelected(false);
            observer.setMode(JLabelBeanCImage.SELECT_LIGNE);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemDessinerLigneActionPerformed

    private void jMenuItemNouvelleRGBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemNouvelleRGBActionPerformed
        JDialogNouvelleCImageRGB dialog = new JDialogNouvelleCImageRGB(this,true);
        dialog.setVisible(true);
        imageRGB = dialog.getCImageRGB();
        observer.setCImage(imageRGB);
        imageNG = null;
        activeMenusRGB();
    }//GEN-LAST:event_jMenuItemNouvelleRGBActionPerformed

    private void jMenuQuitterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuQuitterActionPerformed
        System.exit(0);
    }//GEN-LAST:event_jMenuQuitterActionPerformed


    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new IsilImageProcessing().setVisible(true);
            }
        });
    }

    public void ClicDetected(UnClicEvent e) 
    {
        if (jCheckBoxMenuItemDessinerPixel.isSelected())
        {   
            try 
            {
                if (imageRGB != null) 
                    imageRGB.setPixel(e.getX(),e.getY(),couleurPinceauRGB);
                if (imageNG != null) 
                    imageNG.setPixel(e.getX(),e.getY(),couleurPinceauNG);
            } 
            catch (CImageRGBException ex) 
            { System.out.println("Erreur RGB : " + ex.getMessage()); }
            catch (CImageNGException ex) 
            { System.out.println("Erreur NG : " + ex.getMessage()); }
        }
    }

    public void SelectLigneDetected(DeuxClicsEvent e) 
    {
        if (jCheckBoxMenuItemDessinerLigne.isSelected())
        {   
            try 
            {
                if (imageRGB != null) 
                    imageRGB.DessineLigne(e.getX1(),e.getY1(),e.getX2(),e.getY2(),couleurPinceauRGB);
                if (imageNG != null) 
                    imageNG.DessineLigne(e.getX1(),e.getY1(),e.getX2(),e.getY2(),couleurPinceauNG);
            } 
            catch (CImageRGBException ex) 
            { System.out.println("Erreur RGB : " + ex.getMessage()); }
            catch (CImageNGException ex) 
            { System.out.println("Erreur NG : " + ex.getMessage()); }
        }
    }

    public void SelectRectDetected(DeuxClicsEvent e) 
    {
        if (jCheckBoxMenuItemDessinerRectangle.isSelected())
        {   
            try 
            {
                if (imageRGB != null)
                    imageRGB.DessineRect(e.getX1(),e.getY1(),e.getX2(),e.getY2(),couleurPinceauRGB);
                if (imageNG != null)
                    imageNG.DessineRect(e.getX1(),e.getY1(),e.getX2(),e.getY2(),couleurPinceauNG);
            } 
            catch (CImageRGBException ex) 
            { System.out.println("Erreur RGB : " + ex.getMessage()); }
            catch (CImageNGException ex) 
            { System.out.println("Erreur NG : " + ex.getMessage()); }
        }
    }

    public void SelectCercleDetected(DeuxClicsEvent e) 
    {
        if (jCheckBoxMenuItemDessinerCercle.isSelected())
        {   
            try 
            {
                if (imageRGB != null)
                    imageRGB.DessineCercle(e.getX1(),e.getY1(),e.getX2(),e.getY2(),couleurPinceauRGB);
                if (imageNG != null)
                    imageNG.DessineCercle(e.getX1(),e.getY1(),e.getX2(),e.getY2(),couleurPinceauNG);
            } 
            catch (CImageRGBException ex) 
            { System.out.println("Erreur RGB : " + ex.getMessage()); }
            catch (CImageNGException ex) 
            { System.out.println("Erreur NG : " + ex.getMessage()); }
        }
    }

    public void SelectCercleFillDetected(DeuxClicsEvent e) 
    {
        if (jCheckBoxMenuItemDessinerCerclePlein.isSelected())
        {   
            try 
            {
                if (imageRGB != null)
                    imageRGB.RemplitCercle(e.getX1(),e.getY1(),e.getX2(),e.getY2(),couleurPinceauRGB);
                if (imageNG != null)
                    imageNG.RemplitCercle(e.getX1(),e.getY1(),e.getX2(),e.getY2(),couleurPinceauNG);
            } 
            catch (CImageRGBException ex) 
            { System.out.println("Erreur RGB : " + ex.getMessage()); }
            catch (CImageNGException ex) 
            { System.out.println("Erreur NG : " + ex.getMessage()); }
        }
    }

    public void SelectRectFillDetected(DeuxClicsEvent e) 
    {
        if (jCheckBoxMenuItemDessinerRectanglePlein.isSelected())
        {   
            try 
            {
                if (imageRGB != null) 
                    imageRGB.RemplitRect(e.getX1(),e.getY1(),e.getX2(),e.getY2(),couleurPinceauRGB);
                if (imageNG != null) 
                    imageNG.RemplitRect(e.getX1(),e.getY1(),e.getX2(),e.getY2(),couleurPinceauNG);
            } 
            catch (CImageRGBException ex) 
            { System.out.println("Erreur RGB : " + ex.getMessage()); }
            catch (CImageNGException ex) 
            { System.out.println("Erreur NG : " + ex.getMessage()); }
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroupDessiner;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemDessinerCercle;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemDessinerCerclePlein;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemDessinerLigne;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemDessinerPixel;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemDessinerRectangle;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemDessinerRectanglePlein;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenu jMenuDessiner;
    private javax.swing.JMenu jMenuFourier;
    private javax.swing.JMenu jMenuFourierAfficher;
    private javax.swing.JMenu jMenuHistogramme;
    private javax.swing.JMenuItem jMenuHistogrammeAfficher;
    private javax.swing.JMenu jMenuImage;
    private javax.swing.JMenuItem jMenuItemCouleurPinceau;
    private javax.swing.JMenuItem jMenuItemEnregistrerSous;
    private javax.swing.JMenuItem jMenuItemFourierAfficherModule;
    private javax.swing.JMenuItem jMenuItemFourierAfficherPartieImaginaire;
    private javax.swing.JMenuItem jMenuItemFourierAfficherPartieReelle;
    private javax.swing.JMenuItem jMenuItemFourierAfficherPhase;
    private javax.swing.JMenuItem jMenuItemNouvelleNG;
    private javax.swing.JMenuItem jMenuItemNouvelleRGB;
    private javax.swing.JMenuItem jMenuItemOuvrirNG;
    private javax.swing.JMenuItem jMenuItemOuvrirRGB;
    private javax.swing.JMenu jMenuNouvelle;
    private javax.swing.JMenu jMenuOuvrir;
    private javax.swing.JMenuItem jMenuQuitter;
    private javax.swing.JScrollPane jScrollPane;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;

    // End of variables declaration//GEN-END:variables

    /**
     * Méthode utilitaire pour transposer une matrice int[][].
     * Si l'entrée est [A][B], la sortie est [B][A].
     * @param matrix Matrice d'entrée.
     * @return Nouvelle matrice transposée, ou null si l'entrée est invalide.
     */
    private int[][] transposeMatrix(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) return null;
        int rows = matrix.length;
        int cols = matrix[0].length;
        int[][] transposed = new int[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                transposed[j][i] = matrix[i][j];
            }
        }
        return transposed;
    }


    
}
