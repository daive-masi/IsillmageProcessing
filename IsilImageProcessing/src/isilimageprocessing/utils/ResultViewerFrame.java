package isilimageprocessing.utils;// package isilimageprocessing.utils; // ou package isilimageprocessing;

import CImage.CImage;
import CImage.Observers.JLabelBeanCImage;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ResultViewerFrame extends JFrame {

    public ResultViewerFrame(CImage image, String title) {
        super(title);

        if (image == null || image.getImage() == null) { // Vérifier aussi image.getImage()
            System.err.println("ResultViewerFrame (CImage): Image à afficher est null ou interne BufferedImage est null.");
            setupErrorFrame("Erreur: Image CImage non disponible.");
            return;
        }

        JLabelBeanCImage imageLabel = new JLabelBeanCImage();

        // Tenter de définir une taille préférée pour le bean
        // Cela aide le JScrollPane et le pack() du JFrame
        imageLabel.setPreferredSize(new Dimension(image.getLargeur(), image.getHauteur()));
        imageLabel.setCImage(image); // Mettre l'image APRÈS avoir potentiellement défini la taille

        JScrollPane scrollPane = new JScrollPane(imageLabel);
        // Optionnel: donner une taille préférée au scrollpane aussi, mais pack() devrait suffire
        // scrollPane.setPreferredSize(new Dimension(image.getLargeur() + 20, image.getHauteur() + 20));

        setupFrameBasic(scrollPane);
    }

    /** Surcharge pour afficher directement une BufferedImage */
    public ResultViewerFrame(BufferedImage bImage, String title) {
        super(title);
        if (bImage == null) {
            System.err.println("ResultViewerFrame (BufferedImage): BufferedImage à afficher est null.");
            setupErrorFrame("Erreur: BufferedImage non disponible.");
            return;
        }

        ImageIcon icon = new ImageIcon(bImage);
        JLabel label = new JLabel(icon);
        // Le JLabel avec ImageIcon gère généralement bien sa taille préférée

        JScrollPane scrollPane = new JScrollPane(label);
        // scrollPane.setPreferredSize(new Dimension(bImage.getWidth() + 20, bImage.getHeight() + 20));

        setupFrameBasic(scrollPane);
    }

    private void setupErrorFrame(String errorMessage) {
        add(new JLabel(errorMessage, SwingConstants.CENTER));
        setSize(300, 100);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // Ne pas appeler display() ici car on veut que le constructeur retourne
    }

    private void setupFrameBasic(JComponent content) {
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLayout(new BorderLayout());
        this.add(content, BorderLayout.CENTER);
        this.pack(); // ESSENTIEL: ajuste la taille de la frame au contenu
        this.setLocationByPlatform(true); // Laisser l'OS choisir la position
        // ou this.setLocationRelativeTo(null); pour centrer
    }

    // Méthode pour rendre la fenêtre visible après sa création complète
    public void display() {
        // S'assurer que display est appelé sur l'EDT si la création ne l'était pas déjà.
        // Mais comme on l'appelle depuis SwingUtilities.invokeLater, c'est bon.
        if (!this.isVisible()) { // Éviter de rendre visible plusieurs fois si déjà fait
            this.setVisible(true);
        }
    }
}