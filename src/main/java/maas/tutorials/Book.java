package maas.tutorials;

public class Book {
    private String title;
    private TYPE type;
    private int amount;
    private int price;

    public Book(String title, TYPE type, int amount, int price) {
        this.title = title;
        this.type = type;
        this.amount = amount;
        this.price = price;
    }

    public int getAmount() {
        return amount;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type.toString();
    }

    public int getPrice() {
        return price;
    }

    public void decreaseAmount() {
        --amount;
    }
}
