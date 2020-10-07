package JPEGBaseline;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;

public class JPEGBaseline{
	private int height;
	private int width;
	private BufferedImage inputImage, _outputImage, test;
	private Image testImage;
	private int[] y, cb, cr;
	private ArrayList<int[][]> encodedY, encodedCb, encodedCr;
	
	public BufferedImage returnImage(){
		return _outputImage;
	}
	
	public BufferedImage getYCbCrImage(){ 
		int[] ycbcr = new int[3 * width * height];
		ycbcr = fromRGBtoYCbCr(inputImage);
			
		y = new int[width * height];
		cb = new int[width * height];
		cr = new int[width * height];
		
		int index = 0;
		System.out.println("Preluarea componentei Y");
		for(int i = 0; i < width * height; i ++) 
			y[index ++] = ycbcr[i];


		index = 0;
		System.out.println("Preluarea componentei Cb");
		for(int i = width * height; i < 2 * width * height; i ++) 
			cb[index ++] = ycbcr[i];

		index = 0;
		System.out.println("Preluarea componentei Cr");
		for(int i = 2 * width * height; i < 3 * width * height; i ++)
			cr[index ++] = ycbcr[i];
		index = 0;
		
		_outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		for(int i = 0; i < height; i ++){
			for(int j = 0; j < width; j ++){
				_outputImage.setRGB(j, i, new Color(y[index], cb[index], cr[index++]).getRGB());
			}
		}
		
		return _outputImage;
	}
	
	public ArrayList<int[][]> getYCbCrBlocks(){
		ArrayList<int[][]> blocks = new ArrayList<int[][]>();
		BufferedImage image = getYCbCrImage();
		int []pixels = new int[width * height];
		int index = 0;
		for(int y = 0; y < height; y ++){
			for(int x = 0; x < width; x ++){
				pixels[index ++] = image.getRGB(x, y);
			}
		}
		blocks = getAllBlocks(pixels);
		return blocks;
	}
	
	public ArrayList<int[][]> getEncodedRGBBlocks(){
		System.out.println("Codarea componentei Y");
		encodedY = encode(getAllBlocks(y));
		System.out.println("Codarea componentei Cb");
		encodedCb = encode(getAllBlocks(cb));
		System.out.println("Codarea componentei Cr");
		encodedCr = encode(getAllBlocks(cr));
		int[] _encodedY = fromBlocksToArray(encodedY);
		int[] _encodedCb = fromBlocksToArray(encodedCb);
		int[] _encodedCr = fromBlocksToArray(encodedCr);
		int[] ycbcrEnc = new int[3*width*height];
		
		int index = 0;
		for(int i = 0; i < width*height; i++) ycbcrEnc[i] = _encodedY[index++]; index = 0;
		for(int i = width * height; i < 2 * width * height; i++) ycbcrEnc[i] = _encodedCb[index++]; index = 0;
		for(int i = 2 * width * height; i < 3 * width * height; i++) ycbcrEnc[i] = _encodedCr[index++];

		BufferedImage bimage = fromYCbCrToRGB(ycbcrEnc); //encodedY, encodedCb, encodedCr raman in continuare in YCbCr
		int []array = new int[height * width];
		index = 0;
		for(int y = 0; y < height; y ++){
			for(int x = 0; x < width; x ++){
				array[index ++] = bimage.getRGB(x, y);
			}
		}
		
		ArrayList<int[][]> blocks = new ArrayList<int[][]>();
		blocks = getAllBlocks(array);
		return blocks;
	}
	
	public ArrayList<int[][]> getDecodedRGBBlocks(){
		_outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		ArrayList<int[][]> decodedY = decode(encodedY, 64);
		ArrayList<int[][]> decodedCb = decode(encodedCb, 64);
		ArrayList<int[][]> decodedCr = decode(encodedCr, 64);
		System.out.println("Decodarea componentei Y");
		int[] _decodedY = fromBlocksToArray(decodedY);
		System.out.println("Decodarea componentei Cb");
		int[] _decodedCb = fromBlocksToArray(decodedCb);
		System.out.println("Decodarea componentei Cr");
		int[] _decodedCr = fromBlocksToArray(decodedCr);
		int[] decodedImg = new int[3 * width * height];
		int index = 0;
		for(int i = 0; i < width*height; i++) decodedImg[i] = _decodedY[index++]; index = 0;
		for(int i = width*height; i < 2*width*height; i++) decodedImg[i] = _decodedCb[index++]; index = 0;
		for(int i = 2*width*height; i < 3*width*height; i++) decodedImg[i] = _decodedCr[index++];
		
		BufferedImage bimage = fromYCbCrToRGB(decodedImg);
		int []array = new int[height * width];
		index = 0;
		for(int y = 0; y < height; y ++){
			for(int x = 0; x < width; x ++){
				array[index ++] = bimage.getRGB(x, y);
			}
		}
		
		ArrayList<int[][]> blocks = new ArrayList<int[][]>();
		blocks = getAllBlocks(array);
		return blocks;
	}

	public JPEGBaseline(BufferedImage image, int height, int width){
		inputImage = image;
		this.height = height;
		this.width = width;		
	}
	
	public int[] fromRGBtoYCbCr(BufferedImage image){
		int[] YCbCr = new int[3 * height * width];
		int index = 0;
		int Y, Cb, Cr;
		int r, g, b;
		
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				Color pix = new Color(inputImage.getRGB(x, y));
				r = pix.getRed();
				g = pix.getGreen();
				b = pix.getBlue();
				
				//https://www.w3.org/Graphics/JPEG/jfif3.pdf
				Y = (int)(0.299 * r + 0.587 * g + 0.114 * b);
				Cb = (int)(-0.1687 * r - 0.3313 * g + 0.5 * b + 128);
				Cr = (int)(0.5 * r - 0.4187 * g - 0.0813 * b + 128);	

				YCbCr[index] = Y;
				YCbCr[index + height * width] = Cb;
				YCbCr[index + 2 * height * width] = Cr;
				index ++;
			}
		}
		return YCbCr;
	}
	
	public ArrayList<int[][]> encode(ArrayList<int[][]> blocks){
		ArrayList<int[][]> encodedBlocks = new ArrayList<int[][]>();
		for(int[][] block : blocks){
			encodedBlocks.add((quantize(FDCT(block))));
		}
		return encodedBlocks;
	} 
	
	public ArrayList<int[][]> decode(ArrayList<int[][]> blocks, int n){
		ArrayList<int[][]> decodedBlocks = new ArrayList<int[][]>();
		int index = 0;
		for(int[][] block : blocks){ 
			for(int y = 0; y < 8; y ++){
				for(int x = 0; x < 8; x ++){
					if(index > n) // selecteaza primele n valori din fiecare bloc 
					{
						block[y][x] = 0;
					}
					index ++;
				}
			}
			index = 0;
		}
		for(int[][] block : blocks){
			decodedBlocks.add(IDCT(block));
		}
		return decodedBlocks;
	}
	
	public int[][] IDCT(int[][] block){
		int[][] F = reverseQuantization(block);
		int[][] f = new int[8][8];
		int x, y, u, v;
		double cu, cv, sum;
		
		for (y = 0; y < 8; y ++){
			for (x = 0; x < 8; x ++){
				sum = 0;
				for (v = 0; v < 8; v ++){
					cv = (v == 0) ? 1 / Math.sqrt(2) : 1; 
					for (u = 0; u < 8; u ++){
						cu = (u == 0) ? 1 / Math.sqrt(2) : 1;
						sum += cv * cu * F[v][u] * Math.cos((2 * y + 1) * v * Math.PI / 16.0)
												 * Math.cos((2 * x + 1) * u * Math.PI / 16.0);
					}
				}
				f[y][x] = (int)Math.round(0.25 * sum);// +128;
			}
		}
		return f;
	} 
	
	public int[][] reverseQuantization(int[][] B){
		
		int[][] G = new int[8][8];
		final int[][] Q ={{16, 11, 10, 16, 24,  40,  51,  61},
						  {12, 12, 14, 19, 26,  58,  60,  55},
						  {14, 13, 16, 24, 40,  57,  69,  56},
						  {14, 17, 22, 29, 51,  87,  80,  62},
						  {18, 22, 37, 56, 68,  109, 103, 77},
						  {24, 35, 55, 64, 81,  104, 113, 92},
						  {49, 64, 78, 87, 103, 121, 120, 101},
						  {72, 92, 95, 98, 112, 100, 103, 99}};
		for(int y = 0; y < 8; y ++){
			for(int x = 0; x < 8; x ++){
				G[y][x] = B[y][x] * Q[y][x];
			}
		}
	
		return G;
	}
	
	public int[][] quantize(double[][] dctCoefficients){
		int[][] B = new int[8][8];
		//quantization matrix specified in the original JPEG Standard for a quality of 50%
		//applied for Y (luminance) from YCbCr space
		final int[][] Q ={{16, 11, 10, 16, 24,  40,  51,  61},
						  {12, 12, 14, 19, 26,  58,  60,  55},
						  {14, 13, 16, 24, 40,  57,  69,  56},
						  {14, 17, 22, 29, 51,  87,  80,  62},
						  {18, 22, 37, 56, 68,  109, 103, 77},
						  {24, 35, 55, 64, 81,  104, 113, 92},
						  {49, 64, 78, 87, 103, 121, 120, 101},
						  {72, 92, 95, 98, 112, 100, 103, 99}};
						  
		for(int y = 0; y < 8; y ++){
			for(int x = 0; x < 8; x ++){
				// aici se produce pierderea de informatii
				B[y][x] = (int)Math.round(dctCoefficients[y][x] / Q[y][x]); 
			}
		}
		return B;
	}
	
	public double[][] FDCT(int[][] block){
		double[][] dctCoefficients = new double[8][8];
		double cu, cv, sum;
		int x, y, u, v;
		
		for(v = 0; v < 8; v ++){
			cv = (v == 0) ? 1 / Math.sqrt(2) : 1;
			for(u = 0; u < 8; u ++){
				cu = (u == 0) ? 1 / Math.sqrt(2) : 1;
				sum = 0;
				for(y = 0; y < 8; y ++){
					for(x = 0; x < 8; x ++){
						sum += block[y][x] * Math.cos(((2 * x + 1) * u * Math.PI) / 16)
										   * Math.cos(((2 * y + 1) * v * Math.PI) / 16);
					}
				}
				dctCoefficients[v][u] = Math.round(0.25 * cv * cu * sum * 100.0) / 100.0;
			}
		}
		return dctCoefficients;
	}
	
	public BufferedImage fromYCbCrToRGB(int[] YCbCr){
		BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int index = 0;
		int r, g, b;
		int Y, Cb, Cr;
		
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				
				Y = YCbCr[index];
				Cb = YCbCr[index + width * height];
				Cr = YCbCr[index + 2 * width * height];
				
				//https://www.w3.org/Graphics/JPEG/jfif3.pdf
				r = (int)(Y + 1.402 * (Cr - 128));
				g = (int)(Y - 0.34414 * (Cb - 128) - 0.71414 * (Cr - 128));
				b = (int)(Y + 1.772 * (Cb - 128));
				
				r = Math.max(0, Math.min(r, 255));
				g = Math.max(0, Math.min(g, 255));
				b = Math.max(0, Math.min(b, 255));
				
				Color color = new Color(r, g, b);
				outputImage.setRGB(x, y, color.getRGB());
				index++;
			}
		}
		return outputImage;
	} 

	public int[] fromBlocksToArray(ArrayList<int[][]> blocks){
		int[] array = new int[blocks.size() * 64];
		System.out.println("block size: " + blocks.size());
		int xDisplacement = 0;
		int yDisplacement = 0;

		for(int[][]block : blocks){
			for(int y = 0; y < 8; y++){
				for(int x = 0; x < 8; x++){
					array[x + xDisplacement + (y + yDisplacement) * width] = block[y][x];
				}
			}	
			xDisplacement += 8;
			if(xDisplacement == width){
				xDisplacement = 0;
				yDisplacement += 8;
			}
		}
		return array;
	}

	//a value of 128 is subtracted from each entry to produce a data range that is centered on zero
	//now the range is [-128,127]
	public ArrayList<int[][]> getAllBlocks(int[] array){
		System.out.println("array.length: " + array.length);
		System.out.println("width: " + width + " height: " + height);
		ArrayList<int[][]> blocks = new ArrayList<int[][]>();
		int idx = 0;
		int xDisplacement = 0;
		int yDisplacement = 0;
		int [][] block;
		
		while(idx < array.length){
			block = new int[8][8];
			for(int y = 0; y < 8; y ++){
				for(int x = 0; x < 8; x ++){
					block[y][x] = array[(x + xDisplacement) + (y + yDisplacement) * width];// - 128;
					idx ++;
				}
			}
			xDisplacement += 8;
			if(xDisplacement == width){
				xDisplacement = 0;
				yDisplacement += 8;
			}
			blocks.add(block);
		}
		return blocks;
	}
}