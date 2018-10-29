package maas;

import java.util.List;
import java.util.Vector;
import java.util.Random;
import maas.tutorials.BookBuyerAgent;
import maas.tutorials.BookSellerAgent;
import maas.tutorials.Book;
import maas.tutorials.TYPE;

import static java.lang.Math.round;

public class Start {
	public static final String[] BOOK_TITLES = {
			"Harry Potter and the Philosopher's Stone",
			"Harry Potter and the Chamber of Secrets",
			"Harry Potter and the Prisoner of Azkaban",
			"Harry Potter and the Goblet of Fire"
	};

    public static void main(String[] args) {
    	List<String> buyerAgents = new Vector<>();
		List<String> sellerAgents = new Vector<>();
		Book[][] books = new Book[3][8];

		int numSellerAgents = 3;
		int numBuyerAgents = 20;

		int[] distribution1 = calcRandBookDistribution(4, 20);
        int[] distribution2 = calcRandBookDistribution(4, 20);
        int[] distribution3 = calcRandBookDistribution(4, 20);

        int[][] distributions = {distribution1, distribution2, distribution3};

        for (int i = 0; i < 4; ++i) {
            System.out.println("Seller1 - " + BOOK_TITLES[i] + " - " + distribution1[i]);
            System.out.println("Seller2 - " + BOOK_TITLES[i] + " - " + distribution2[i]);
            System.out.println("Seller3 - " + BOOK_TITLES[i] + " - " + distribution3[i]);
        }
		for (int i = 0; i < numSellerAgents; ++i) {
			for (int j = 0; j < 4; ++j) {
				books[i][j * 2] = new Book(BOOK_TITLES[j], TYPE.PAPERBACK, distributions[i][j], new Random().nextInt(50));
				books[i][j * 2 + 1] = new Book(BOOK_TITLES[j], TYPE.EBOOK, -1, new Random().nextInt(50));
			}
		}

		for (int i = 0; i < numBuyerAgents; ++i) {
		    int r = new Random().nextInt(3);
            int c = new Random().nextInt(8);
			buyerAgents.add("buyerAgent" + i +
                    ":maas.tutorials.BookBuyerAgent" +
                    "(" + books[r][c].getTitle() + "_" + books[r][c].getType() + ")");
		}

		StringBuilder sellerStringBuilder = new StringBuilder();
		for (int i = 0; i < numSellerAgents; ++i) {
		    sellerStringBuilder.append("sellerAgent" + i + ":maas.tutorials.BookSellerAgent(");
		    for (int j = 0; j < books[i].length; ++j) {
                sellerStringBuilder.append(books[i][j].getTitle() + "#");
                sellerStringBuilder.append(books[i][j].getType() + "#");
                sellerStringBuilder.append(books[i][j].getAmount() + "#");
                sellerStringBuilder.append(books[i][j].getPrice() + ",");
            }
            sellerStringBuilder.append(")");
		    String sellerString = sellerStringBuilder.toString().replaceAll(",\\)", "\\)");
			sellerAgents.add(sellerString);
            sellerStringBuilder = new StringBuilder();
		}

    	List<String> cmd = new Vector<>();
    	cmd.add("-agents");
    	StringBuilder sb = new StringBuilder();
    	for (String a : buyerAgents) {
    		sb.append(a);
    		sb.append(";");
    	}
		for (String a : sellerAgents) {
			sb.append(a);
			sb.append(";");
		}
    	cmd.add(sb.toString());
    	System.out.println(cmd.toString());
        jade.Boot.main(cmd.toArray(new String[cmd.size()]));
    }

    public static int[] calcRandBookDistribution(int numBookTitles, int maxBooks) {
    	Random numGen = new Random();
    	int[] randIntNums = new int[numBookTitles];
		int[] finalNums = new int[numBookTitles];
		int sum = 0;
    	for (int i = 0; i < numBookTitles; ++i) {
    		randIntNums[i] = numGen.nextInt(20);
    		sum += randIntNums[i];
		}
		if (sum == maxBooks) {
    		return randIntNums;
		}
		int newSum = 0;
		for (int i = 0; i < numBookTitles; ++i) {
			finalNums[i] = round((((float)randIntNums[i]) / sum) * maxBooks);
			newSum += finalNums[i];
		}
		while (newSum != maxBooks) {
			int pos = numGen.nextInt(numBookTitles);
			if (newSum  > maxBooks) {
				if (finalNums[pos] > 0) {
					finalNums[pos]--;
					newSum--;
				}
			}
			else {
				finalNums[pos]++;
				newSum++;
			}
		}
		return finalNums;
	}
}
