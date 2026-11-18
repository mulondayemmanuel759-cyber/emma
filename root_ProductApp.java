import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

/**
 * One-file mattress sales application.
 *
 * Compile:
 *   javac ProductApp.java
 *
 * Run:
 *   java ProductApp
 *
 * Run the built-in calculation checks:
 *   java ProductApp --test
 */
public class ProductApp extends JFrame {
    private static final String DEFAULT_SELECTION = "Select Product";
    private static final List<Product> PRODUCTS = Arrays.asList(
            new Product("Cozy Nights Monarch", 3500.00),
            new Product("Restonic Barbados", 3000.00),
            new Product("Sealy Phuket", 2800.00)
    );

    private final DecimalFormat moneyFormat = new DecimalFormat("R 0.00");
    private final JTextField customerNameField = new JTextField(22);
    private final JComboBox<String> productComboBox = new JComboBox<>();
    private final JTextField priceField = new JTextField(22);
    private final JTextField vatField = new JTextField(22);
    private final JTextField commissionField = new JTextField(22);
    private final JTextField totalField = new JTextField(22);
    private final JTextArea reportArea = new JTextArea(14, 58);

    public ProductApp() {
        super("Product App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(760, 650);
        setLocationRelativeTo(null);
        setJMenuBar(createMenuBar());
        setContentPane(createContentPanel());
        clearFields();
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(event -> dispose());
        fileMenu.add(exitItem);

        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem processItem = new JMenuItem("Process");
        processItem.addActionListener(event -> processSale());
        JMenuItem clearItem = new JMenuItem("Clear");
        clearItem.addActionListener(event -> clearFields());
        JMenuItem reportItem = new JMenuItem("Sales Report");
        reportItem.addActionListener(event -> displaySalesReport());
        toolsMenu.add(processItem);
        toolsMenu.add(clearItem);
        toolsMenu.addSeparator();
        toolsMenu.add(reportItem);

        menuBar.add(fileMenu);
        menuBar.add(toolsMenu);
        return menuBar;
    }

    private JPanel createContentPanel() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Sale Details"));

        productComboBox.addItem(DEFAULT_SELECTION);
        for (Product product : PRODUCTS) {
            productComboBox.addItem(product.name());
        }
        productComboBox.addActionListener(event -> updatePrice());

        addRow(inputPanel, 0, "Customer Name:", customerNameField);
        addRow(inputPanel, 1, "Select Product:", productComboBox);
        addRow(inputPanel, 2, "Enter Price:", priceField);
        addRow(inputPanel, 3, "VAT Amount:", vatField);
        addRow(inputPanel, 4, "Commission Amount:", commissionField);
        addRow(inputPanel, 5, "Total Sale Amount:", totalField);

        JPanel buttonPanel = new JPanel();
        JButton processButton = new JButton("PROCESS");
        processButton.addActionListener(event -> processSale());
        JButton clearButton = new JButton("CLEAR");
        clearButton.addActionListener(event -> clearFields());
        buttonPanel.add(processButton);
        buttonPanel.add(clearButton);

        reportArea.setEditable(false);
        reportArea.setLineWrap(true);
        reportArea.setWrapStyleWord(true);
        JScrollPane reportScrollPane = new JScrollPane(reportArea);
        reportScrollPane.setBorder(BorderFactory.createTitledBorder("Sales Report"));

        root.add(inputPanel, BorderLayout.NORTH);
        root.add(reportScrollPane, BorderLayout.CENTER);
        root.add(buttonPanel, BorderLayout.SOUTH);
        return root;
    }

    private void addRow(JPanel panel, int row, String labelText, java.awt.Component component) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.LINE_END;
        labelConstraints.insets = new Insets(6, 6, 6, 10);
        panel.add(new JLabel(labelText), labelConstraints);

        GridBagConstraints componentConstraints = new GridBagConstraints();
        componentConstraints.gridx = 1;
        componentConstraints.gridy = row;
        componentConstraints.weightx = 1;
        componentConstraints.fill = GridBagConstraints.HORIZONTAL;
        componentConstraints.insets = new Insets(6, 6, 6, 6);
        panel.add(component, componentConstraints);
    }

    private void updatePrice() {
        Product product = selectedProduct();
        priceField.setText(product == null ? "" : formatMoney(product.price()));
    }

    private Product selectedProduct() {
        int selectedIndex = productComboBox.getSelectedIndex() - 1;
        return selectedIndex >= 0 && selectedIndex < PRODUCTS.size()
                ? PRODUCTS.get(selectedIndex)
                : null;
    }

    private void processSale() {
        String customerName = customerNameField.getText().trim();
        Product product = selectedProduct();

        if (customerName.isEmpty()) {
            showError("Please enter the customer's name.");
            return;
        }
        if (product == null) {
            showError("Please select a product.");
            return;
        }

        double price;
        try {
            price = parseAmount(priceField.getText());
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
            return;
        }

        ProductCalculations calculations = new ProductCalculations(price);
        double vat = calculations.getVat();
        double commission = calculations.getCommission();
        double total = calculations.getTotal();

        vatField.setText(formatMoney(vat));
        commissionField.setText(formatMoney(commission));
        totalField.setText(formatMoney(total));

        try {
            appendSaleToFile(customerName, product.name(), vat, commission, total);
            JOptionPane.showMessageDialog(
                    this,
                    "Sale processed and saved to data.txt.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (IOException exception) {
            showError("The sale was calculated, but data.txt could not be saved: "
                    + exception.getMessage());
        }
    }

    private double parseAmount(String text) {
        String cleaned = text.trim().replace("R", "").replace(",", "");
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Please enter a valid product price.");
        }

        try {
            double amount = Double.parseDouble(cleaned);
            if (!Double.isFinite(amount) || amount < 0) {
                throw new NumberFormatException();
            }
            return amount;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Please enter a valid non-negative price.");
        }
    }

    private void appendSaleToFile(
            String customerName,
            String productName,
            double vat,
            double commission,
            double total
    ) throws IOException {
        Path dataFile = Path.of("data.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(
                dataFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            writer.write("DATA LOG");
            writer.newLine();
            writer.write("************************");
            writer.newLine();
            writer.write("Customer Name: " + customerName);
            writer.newLine();
            writer.write("Product: " + productName);
            writer.newLine();
            writer.write("VAT: " + formatMoney(vat));
            writer.newLine();
            writer.write("COMMISSION: " + formatMoney(commission));
            writer.newLine();
            writer.write("TOTAL: " + formatMoney(total));
            writer.newLine();
            writer.write("************************");
            writer.newLine();
            writer.newLine();
        }
    }

    private void displaySalesReport() {
        SalesReport report = new SalesReport(new int[][]{
                {25, 18, 20},
                {15, 10, 12},
                {18, 15, 20}
        });
        reportArea.setText(report.toText());
    }

    private void clearFields() {
        customerNameField.setText("");
        productComboBox.setSelectedIndex(0);
        priceField.setText("");
        vatField.setText("");
        commissionField.setText("");
        totalField.setText("");
        reportArea.setText("");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input Error", JOptionPane.ERROR_MESSAGE);
    }

    private String formatMoney(double amount) {
        return moneyFormat.format(amount);
    }

    private static void runSelfTests() {
        ProductCalculations calculations = new ProductCalculations(3500.00);
        assertClose(525.00, calculations.getVat(), "VAT");
        assertClose(175.00, calculations.getCommission(), "commission");
        assertClose(4200.00, calculations.getTotal(), "total");

        SalesReport report = new SalesReport(new int[][]{
                {25, 18, 20},
                {15, 10, 12},
                {18, 15, 20}
        });
        if (report.totalSales() != 153) {
            throw new AssertionError("Total sales should be 153.");
        }
        if (report.averageSales() != 17) {
            throw new AssertionError("Average sales should be 17.");
        }
        if (!"Cozy Nights Monarch".equals(report.mostPopularProduct())) {
            throw new AssertionError("Cozy Nights Monarch should be most popular.");
        }
        System.out.println("All tests passed.");
    }

    private static void assertClose(double expected, double actual, String name) {
        if (Math.abs(expected - actual) > 0.000001) {
            throw new AssertionError(name + " expected " + expected + " but was " + actual);
        }
    }

    public static void main(String[] args) {
        if (args.length == 1 && "--test".equals(args[0])) {
            runSelfTests();
            return;
        }

        SwingUtilities.invokeLater(() -> new ProductApp().setVisible(true));
    }

    private record Product(String name, double price) {
    }

    public interface IProductCalculations {
        double getVat();

        double getCommission();

        double getTotal();
    }

    public static final class ProductCalculations implements IProductCalculations {
        private static final double VAT_RATE = 0.15;
        private static final double COMMISSION_RATE = 0.05;
        private final double productPrice;

        public ProductCalculations(double productPrice) {
            if (!Double.isFinite(productPrice) || productPrice < 0) {
                throw new IllegalArgumentException("Product price must be non-negative.");
            }
            this.productPrice = productPrice;
        }

        @Override
        public double getVat() {
            return productPrice * VAT_RATE;
        }

        @Override
        public double getCommission() {
            return productPrice * COMMISSION_RATE;
        }

        @Override
        public double getTotal() {
            return productPrice + getVat() + getCommission();
        }
    }

    public static final class SalesReport {
        private static final String[] PRODUCT_NAMES = {
                "Cozy Nights Monarch",
                "Restonic Barbados",
                "Sealy Phuket"
        };
        private final int[][] monthlySales;

        public SalesReport(int[][] monthlySales) {
            if (monthlySales == null || monthlySales.length != 3) {
                throw new IllegalArgumentException("Exactly three months are required.");
            }
            this.monthlySales = copyAndValidate(monthlySales);
        }

        public int totalSales() {
            int total = 0;
            for (int[] month : monthlySales) {
                for (int sales : month) {
                    total += sales;
                }
            }
            return total;
        }

        public int averageSales() {
            return totalSales() / (monthlySales.length * PRODUCT_NAMES.length);
        }

        public String mostPopularProduct() {
            int mostPopularIndex = 0;
            int highestTotal = Integer.MIN_VALUE;
            for (int productIndex = 0; productIndex < PRODUCT_NAMES.length; productIndex++) {
                int productTotal = 0;
                for (int[] month : monthlySales) {
                    productTotal += month[productIndex];
                }
                if (productTotal > highestTotal) {
                    highestTotal = productTotal;
                    mostPopularIndex = productIndex;
                }
            }
            return PRODUCT_NAMES[mostPopularIndex];
        }

        public String toText() {
            StringBuilder output = new StringBuilder("PRODUCT SALES REPORT - 2026\n\n");
            for (int monthIndex = 0; monthIndex < monthlySales.length; monthIndex++) {
                for (int productIndex = 0; productIndex < PRODUCT_NAMES.length; productIndex++) {
                    output.append("Enter number of sales for month ")
                            .append(monthIndex + 1)
                            .append(" for ")
                            .append(PRODUCT_NAMES[productIndex])
                            .append(" mattresses: ")
                            .append(monthlySales[monthIndex][productIndex])
                            .append('\n');
                }
            }
            output.append("\n******************************\n");
            output.append("TOTAL PRODUCT SALES: ").append(totalSales()).append('\n');
            output.append("AVERAGE PRODUCT SALES: ").append(averageSales()).append('\n');
            output.append("MOST POPULAR PRODUCT: ").append(mostPopularProduct()).append('\n');
            return output.toString();
        }

        private static int[][] copyAndValidate(int[][] source) {
            int[][] copy = new int[source.length][];
            for (int row = 0; row < source.length; row++) {
                if (source[row] == null || source[row].length != PRODUCT_NAMES.length) {
                    throw new IllegalArgumentException(
                            "Each month must contain sales for all three products."
                    );
                }
                copy[row] = source[row].clone();
                for (int sales : copy[row]) {
                    if (sales < 0) {
                        throw new IllegalArgumentException("Sales values cannot be negative.");
                    }
                }
            }
            return copy;
        }
    }
}