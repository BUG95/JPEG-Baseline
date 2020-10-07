import JPEGBaseline.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.*;
import java.io.*;
import java.util.*;

class DrawningBoard extends Canvas implements Runnable{
	
	public DrawningBoard board1;
	public JLabel fileLength, fileName, imageWidthLabel, imageHeightLabel, thisStep, nextStep, compressState;
	private Image image, image2;
	private int imageWidth, imageHeight;
	private BufferedImage bImage;
	public Thread thread;
	private int []pixels;
	public boolean isBoard1 = false, isBoard2 = false, drawLabelsBoard1 = false, drawLabelsBoard2 = false;
	public boolean drawImageOnBoard2 = false, clearBoard2 = false, drawCompressState = false, nok = false;
	public boolean originalImageIsReconstructed = false;
	
	public void copyBoard1(DrawningBoard board1){
		this.board1 = board1;
	}
	
	public void cleanPixelsVector(){
		if(pixels != null){
			for(int i = 0; i < pixels.length; i ++) 
				pixels[i] = 0;
		}
	}
	
	public void setBoard1(boolean val){
		isBoard1 = val;
	}
	
	public void setBoard2(boolean val){
		isBoard2 = val;
	}
	
	public void setCompressStep(String step){
		compressState = new JLabel("Compress state: " + step + fileName.getText().substring(10));
	}
	
	public void setThisStep(String step){
		thisStep = new JLabel("This step: " + step);
	}
	
	public void setNextStep(String step){
		nextStep = new JLabel("Next step: " + step);
	}
	
	public void setFileLengthLabel(File file){
		double bytes = file.length();
		fileLength = new JLabel("File size: " + Math.round(bytes / 1024) + " KB");
	}
	
	public void setFileNameLabel(File file){
		fileName = new JLabel("File name: " + file.getName());
	}
	
	public void setImageHeightLabel(BufferedImage img){
		imageHeightLabel = new JLabel("Image height: " + img.getHeight());
	}
	
	public void setImageWidthLabel(BufferedImage img){
		imageWidthLabel = new JLabel("Image width: " + img.getWidth());
	}
	
	public void setSomeTo(DrawningBoard board2){
		board2.image = image;
		board2.image2 = image2;
		board2.imageWidth = imageWidth;
		board2.imageHeight = imageHeight;
		board2.bImage = bImage;
	}
	
	public void setImage(File file, int board){
		try{
			image = ImageIO.read(file);
			bImage = ImageIO.read(file);
			imageWidth = bImage.getWidth();
			imageHeight = bImage.getHeight();
			nok = false;
			if((imageHeight % 8 != 0) || (imageWidth % 8 != 0)){
				nok = true;
			}
			
		}
		catch(IOException e){System.out.println("Eroare la incarcarea imaginii!");}
		
		if(board == 0){
			setFileLengthLabel(file);
			setFileNameLabel(file);
			setImageHeightLabel(bImage);
			setImageWidthLabel(bImage);
			drawLabelsBoard1 = true;
		}
		
		if(board == 1){
			setThisStep("Imaginea originala");
			setNextStep("");
			drawLabelsBoard2 = true;
			drawImageOnBoard2 = true;
			cleanPixelsVector();
		}
	}
	
	
	public BufferedImage getOutputImage(int[] pixels){
		BufferedImage outputImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		int index = 0;
		for(int y = 0; y < imageHeight; y ++){
			for(int x = 0; x < imageWidth; x ++){
				outputImage.setRGB(x, y, pixels[index ++]);
			}
		}
		return outputImage;
	}
	
	public void run() {
		BufferedImage outputImage;
		File imagePath = null;
		originalImageIsReconstructed = false;
		repaint();
		setBoard1(false);
		setBoard2(true);
		pixels = new int[imageHeight * imageWidth];
		JPEGBaseline dct = new JPEGBaseline(bImage, imageHeight, imageWidth);
		setThisStep("Imaginea originala este transformata din spatiul de culori RGB in spatiul de culori YCbCr");
		setNextStep("Fiecare canal (Y, Cb, Cr) este impartit in blocuri de 8x8 apoi fiecarui bloc i se aplica DCT");
		repaint();
		ArrayList<int[][]> blocks = dct.getYCbCrBlocks();
		int xx = 0, yy = 0, xmax = 8, ymax = 8;
		for(int[][] block : blocks){
			for(int y = 0; y < 8; y ++){
				for(int x = 0; x < 8; x ++){
					pixels[(xx + x) + (yy + y) * imageWidth] = block[y][x];
				}
			}
			xx += 8;
			if(xx == imageWidth){
				xx = 0;
				yy += 8;
			}
			try{thread.sleep(5);} 
			catch(InterruptedException e){System.out.println("Eroare la firul de executie");}
			repaint();
		}
		outputImage = getOutputImage(pixels);
		try{
			imagePath = new File("temp/fromYCbCrToRGB.jpg");
			ImageIO.write(outputImage, "jpg", imagePath);
		} catch(IOException ioex){}
		setThisStep("Fiecare canal (Y, Cb, Cr) este impartit in blocuri de 8x8 apoi fiecarui bloc i se aplica DCT");
		setNextStep("Imaginea din spatiul de culori YCbCr codificata");
		clearBoard2 = true;
		
		xx = 0; yy = 0; xmax = 8; ymax = 8;
		blocks = dct.getEncodedRGBBlocks();
		for(int[][] block : blocks){
			for(int y = 0; y < 8; y ++){
				for(int x = 0; x < 8; x ++){
					pixels[(xx + x) + (yy + y) * imageWidth] = block[y][x];
				}
			}
			xx += 8;
			if(xx == imageWidth){
				xx = 0;
				yy += 8;
			}
			try{thread.sleep(5);} 
			catch(InterruptedException e){System.out.println("Eroare la firul de executie");}
			repaint();
		}
		outputImage = getOutputImage(pixels);
		try{
			imagePath = new File("temp/encodedRGB.jpg");
			ImageIO.write(outputImage, "jpg", imagePath);
		} catch(IOException ioex){}


		setThisStep("Imaginea din spatiul de culori YCbCr codificata");
		setNextStep("Imaginea se decodifica, aplicandu-i-se IDCT, apoi se transforma din spatiul YCbCr in RGB");
		clearBoard2 = true;
		
		xx = 0; yy = 0; xmax = 8; ymax = 8;
		blocks = dct.getDecodedRGBBlocks();
		for(int[][] block : blocks){
			for(int y = 0; y < 8; y ++){
				for(int x = 0; x < 8; x ++){
					pixels[(xx + x) + (yy + y) * imageWidth] = block[y][x];
				}
			}
			xx += 8;
			if(xx == imageWidth){
				xx = 0;
				yy += 8;
			}
			try{thread.sleep(5);} 
			catch(InterruptedException e){System.out.println("Eroare la firul de executie");}
			repaint();
		} 
		outputImage = getOutputImage(pixels);
		try{
			imagePath = new File("temp/output.jpg");
			ImageIO.write(outputImage, "jpg", imagePath);
		} catch(IOException ioex){}
		setThisStep("Imaginea reconstruita");
		setNextStep("Pentru incarcarea altei imagini este nevoie de activarea etichetei Stop din meniul Compress");
		clearBoard2 = true;
		board1.setCompressStep("S-a incheiat compresia fisierului");
		board1.repaint();
		repaint();
		if(imagePath != null)
			setFileLengthLabel(imagePath);
		originalImageIsReconstructed = true;
	}

	public void paint(Graphics g){
		g.setFont(new Font("SansSerif", Font.BOLD, 13));
		if(isBoard1){
			g.clearRect(0, 0, this.getWidth(), this.getHeight()); 
			g.drawImage(image, 20, 0, this);
			if(drawLabelsBoard1){
				g.drawString(fileLength.getText(), 20, imageHeight + 15);
				g.drawString(fileName.getText(), 20, imageHeight + 30);
				g.drawString(imageHeightLabel.getText(), 20, imageHeight + 45);
				g.drawString(imageWidthLabel.getText(), 20, imageHeight + 60);
			}
			if(drawCompressState){
				g.drawString(compressState.getText(), 20, imageHeight + 75);
			}
		}
		else if(isBoard2){
			if(clearBoard2){g.clearRect(0, 0, this.getWidth(), this.getHeight()); clearBoard2 = false;}
			if(drawImageOnBoard2){
				g.drawImage(image, 20, 0, this);
				drawImageOnBoard2 = false;
			}	
			//if(clearLabelsBoard2){}
			image2 = createImage(new MemoryImageSource(imageWidth, imageHeight, pixels, 0, imageWidth));
			g.drawImage(image2, 20, 0, this);
			if(drawLabelsBoard2){
				g.drawString(thisStep.getText(), 20, imageHeight + 15);
				g.drawString(nextStep.getText(), 20, imageHeight + 30);
			}
			if(originalImageIsReconstructed){
				g.drawString(fileLength.getText(), 20, imageHeight + 45); // aici: output file size
			}
		}
	}
	
	public void update(Graphics g){
		paint(g);
	}
} 

public class GUI extends JFrame implements ActionListener{
	
	private DrawningBoard board1, board2;
	private Dimension dim;
	private JMenuItem add_image_item, start_item, stop_item, exit_item;
	
	private void addActionEvents(){
		add_image_item.addActionListener(this);
		start_item.addActionListener(this);
		stop_item.addActionListener(this);
		exit_item.addActionListener(this);
	}
	
	private void addMenuBar(){
		JMenu menu1 = new JMenu("Menu");
		JMenuBar mb = new JMenuBar();
		add_image_item = new JMenuItem("Add image file");
		//addSeparator();
		exit_item = new JMenuItem("Exit");
		menu1.add(add_image_item);
		menu1.addSeparator();
		menu1.add(exit_item);
		mb.add(menu1);
		
		JMenu menu2 = new JMenu("Compress");
		start_item = new JMenuItem("Start");
		stop_item = new JMenuItem("Stop");
		menu2.add(start_item);
		menu2.add(stop_item);
		mb.add(menu2);
		
		menu1.setFont(new Font("Arial", Font.BOLD, 16));
		menu2.setFont(new Font("Arial", Font.BOLD, 16));
		
		addActionEvents();
		
		setJMenuBar(mb);
		
		stop_item.setEnabled(false);
		start_item.setEnabled(false);
	}
	
	public GUI(String title){
		super(title);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		setUndecorated(true);
		dim = Toolkit.getDefaultToolkit().getScreenSize();
		setSize(dim);
		
		addMenuBar();
		
		setLayout(new GridLayout(1,2));
		board1 = new DrawningBoard();
		board2 = new DrawningBoard();
		add(board1);
		add(board2);
		
		setVisible(true);
		
	}
	
	public void actionPerformed(ActionEvent e){
		String command = e.getActionCommand();
		if(command.equals("Add image file")) {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setCurrentDirectory(new File("./images"));
			int result = fileChooser.showOpenDialog(this);
			if (result == JFileChooser.APPROVE_OPTION) {
				board2.clearBoard2 = true;
				File imagePath = fileChooser.getSelectedFile();
				board1.setBoard1(true);
				board1.setImage(imagePath, 0); // 0 = board1
				board1.repaint();
				board2.setBoard2(true);
				board2.setImage(imagePath, 1); // 1 = board2
				board2.repaint();
				start_item.setEnabled(true);
				if(board1.drawCompressState == true) 
					board1.drawCompressState = false;
				board2.copyBoard1(board1);
			}
		}
		else if(command.equals("Start")){
			board2.clearBoard2 = true;
			board2.drawImageOnBoard2 = true;
			if(board1.nok){
				JOptionPane.showMessageDialog(this, "Inaltimea / latimea imaginii nu se imparte la 8 (nu se pot construi blocuri 8x8 pixeli)");
			} else {
				board1.setSomeTo(board2);
				board2.thread = new Thread(board2);
				board2.thread.start();
				start_item.setEnabled(false);
				stop_item.setEnabled(true);
				add_image_item.setEnabled(false);
				board1.drawCompressState = true;
				board1.setCompressStep("A inceput compresia fisierului");
				board2.cleanPixelsVector();
				board1.repaint();
				board2.repaint();
			}
		}
		else if(command.equals("Stop")){
			board2.thread.stop();
			start_item.setEnabled(true);
			stop_item.setEnabled(false);
			add_image_item.setEnabled(true);
			if(!board2.originalImageIsReconstructed)
				board1.setCompressStep("S-a intrerupt compresia fisierului");
			board1.repaint();
		}
		else if(command.equals("Exit")){
			System.exit(0);
		}
	}
	public static void main(String []args){
		new GUI("EduJPEG");
	}
}