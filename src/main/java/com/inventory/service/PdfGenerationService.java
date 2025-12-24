package com.inventory.service;

import com.inventory.dto.PowderCoatingProcessPdfDto;
import com.inventory.entity.Customer;
import com.inventory.entity.PowderCoatingProcess;
import com.inventory.entity.PowderCoatingProcessItem;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.CustomerRepository;
import com.inventory.repository.PowderCoatingProcessRepository;
import com.inventory.repository.PowderCoatingProcessItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGenerationService {
    // Color scheme as per requirements
    private static final Color PRIMARY_COLOR = new DeviceRgb(245, 106, 73);     // #f56a49
    private static final Color SECONDARY_COLOR = new DeviceRgb(0, 63, 105);     // #003f69
    private static final Color PRIMARY_LIGHT = new DeviceRgb(255, 139, 115);    // #ff8b73
    private static final Color SECONDARY_LIGHT = new DeviceRgb(0, 92, 158);     // #005c9e
    private static final Color TEXT_DARK = new DeviceRgb(51, 51, 51);           // #333333
    private static final Color TEXT_LIGHT = new DeviceRgb(255, 255, 255);       // #ffffff
    private static final Color BACKGROUND_LIGHT = new DeviceRgb(245, 245, 245); // #f5f5f5

    private final PowderCoatingProcessRepository processRepository;
    private final PowderCoatingProcessItemRepository processItemRepository;
    private final CustomerRepository customerRepository;
    private final UtilityService utilityService;

    public byte[] generateEstimatePdf(PowderCoatingProcessPdfDto dto) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ValidationException("Customer not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(!customer.getClient().getId().equals(currentUser.getClient().getId())) {
                throw new ValidationException("You are not authorized to view this customer");
            }

            List<PowderCoatingProcess> processes = processRepository.findAllById(dto.getProcessIds());

            try (PdfDocument pdf = new PdfDocument(new PdfWriter(outputStream))) {
                Document document = new Document(pdf, PageSize.A4);
                document.setMargins(36, 36, 36, 36);

                // Get the earliest created_at date from processes
                java.time.OffsetDateTime processDate = processes.stream()
                    .map(PowderCoatingProcess::getCreatedAt)
                    .filter(date -> date != null)
                    .min(java.time.OffsetDateTime::compareTo)
                    .orElse(java.time.OffsetDateTime.now());

                // Add content
                addHeader(document, processDate);
                addCustomerDetails(document, customer, processDate);
                addProcessTable(document, processes);
                addTotal(document, processes);
                addFooter(pdf, document);
                addSignature(document);

                document.close();
            }
            
            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error generating PDF", e);
            throw new ValidationException("Failed to generate PDF: " + e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void addHeader(Document document, java.time.OffsetDateTime processDate) {
        // Add logo in the center
        Table logoTable = new Table(1).useAllAvailableWidth();
        Cell logoCell = new Cell();
        try {
            InputStream imageStream = getClass().getClassLoader().getResourceAsStream("quotation/jk_logo.png");
            if (imageStream == null) {
                log.error("Image not found: quotation/jk_logo.png");
                throw new FileNotFoundException("Image not found: quotation/jk_logo.png");
            }
            log.info("Successfully loaded logo image: quotation/jk_logo.png");
            ImageData imageData = ImageDataFactory.create(imageStream.readAllBytes());
            Image img = new Image(imageData);
            img.setWidth(200);
            img.setHeight(50);
            img.setHorizontalAlignment(HorizontalAlignment.CENTER);
            logoCell.add(img);
        } catch (Exception e) {
            log.error("Error loading logo image: quotation/jk_logo.png", e);
        }
        logoCell.setBorder(new SolidBorder(SECONDARY_COLOR, 1))
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setPadding(5)
                .setTextAlignment(TextAlignment.CENTER);
        logoTable.addCell(logoCell);
        document.add(logoTable);

        // Company details - 2 column layout
        Table contentTable = new Table(new float[]{1, 1}).useAllAvailableWidth();
        
        // Left side - Address, Email, Mobile
        Cell leftDetailsCell = new Cell();
        leftDetailsCell.add(new Paragraph("Address :- Radhekrishan Chowk, Sojitra park, Mavdi,")
                        .setFontSize(9)
                        .setFontColor(TEXT_DARK))
                .add(new Paragraph("baypass road, Dist. Rajkot, Gujarat - 360005")
                        .setFontSize(9)
                        .setFontColor(TEXT_DARK))
                .add(new Paragraph("E-mail: jkindustries1955@gmail.com")
                        .setFontSize(9)
                        .setFontColor(TEXT_DARK))
                .add(new Paragraph("Mo.No. 9979032430")
                        .setFontSize(9)
                        .setFontColor(TEXT_DARK))
                .setBorder(new SolidBorder(PRIMARY_COLOR, 1))
                .setBorderRight(Border.NO_BORDER)
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setPadding(8)
                .setTextAlignment(TextAlignment.LEFT);
        
        // Right side - GST
        Cell rightDetailsCell = new Cell();
        rightDetailsCell.add(new Paragraph("GST NO:24AAMFJ9388A1Z4")
                        .setFontSize(10)
                        .setBold()
                        .setFontColor(PRIMARY_COLOR))
                .setBorder(new SolidBorder(PRIMARY_COLOR, 1))
                .setBorderLeft(Border.NO_BORDER)
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setPadding(8)
                .setTextAlignment(TextAlignment.LEFT)
                .setVerticalAlignment(VerticalAlignment.BOTTOM);

        contentTable.addCell(leftDetailsCell);
        contentTable.addCell(rightDetailsCell);
        document.add(contentTable);

        // Add styled estimate heading
        Table estimateHeadingTable = new Table(1).useAllAvailableWidth();
        Cell estimateHeadingCell = new Cell()
                .add(new Paragraph("J.K INDUSTRIES ESTIMATE")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(12)
                        .setBold()
                        .setFontColor(TEXT_LIGHT))
                .setBackgroundColor(PRIMARY_COLOR)
                .setPadding(8);
        estimateHeadingTable.addCell(estimateHeadingCell);
        
        document.add(new Paragraph("\n").setFontSize(2));
        document.add(estimateHeadingTable);
        document.add(new Paragraph("\n").setFontSize(2));
    }

    private void addCustomerDetails(Document document, Customer customer, java.time.OffsetDateTime processDate) {
        // Create a 2-column table for customer details
        Table infoTable = new Table(new float[]{1, 1}).useAllAvailableWidth();
        
        // Left side - Customer information
        Cell leftCell = new Cell();
        Paragraph customerName = new Paragraph(customer.getName())
                .setBold()
                .setFontSize(9)
                .setFontColor(PRIMARY_COLOR);
        
        leftCell.add(new Paragraph("To,").setFontSize(8).setFontColor(TEXT_DARK))
                .add(customerName);
        
        // Address on first line
        if (customer.getAddress() != null) {
            leftCell.add(new Paragraph(customer.getAddress()).setFontSize(8).setFontColor(TEXT_DARK));
        }
        
        // Mobile number on new line
        if (customer.getMobile() != null) {
            leftCell.add(new Paragraph("Mo.No. " + customer.getMobile()).setFontSize(8).setFontColor(TEXT_DARK));
        }
        
        leftCell.setBackgroundColor(BACKGROUND_LIGHT)
                .setBorder(new SolidBorder(SECONDARY_LIGHT, 1))
                .setBorderRight(Border.NO_BORDER)
                .setPadding(8)
                .setTextAlignment(TextAlignment.LEFT);
        
        // Right side - GST and Date (left-aligned)
        Cell rightCell = new Cell();
        String dateStr = processDate.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        
        // GST number
        if (customer.getGst() != null) {
            rightCell.add(new Paragraph("GST No: " + customer.getGst()).setFontSize(8).setFontColor(TEXT_DARK));
        }
        
        // Date on new line
        rightCell.add(new Paragraph("Date: " + dateStr).setFontSize(8).setFontColor(TEXT_DARK));
        
        rightCell.setBackgroundColor(BACKGROUND_LIGHT)
                .setBorder(new SolidBorder(SECONDARY_LIGHT, 1))
                .setBorderLeft(Border.NO_BORDER)
                .setPadding(8)
                .setTextAlignment(TextAlignment.LEFT)
                .setVerticalAlignment(VerticalAlignment.BOTTOM);
        
        infoTable.addCell(leftCell);
        infoTable.addCell(rightCell);
        document.add(infoTable);
        document.add(new Paragraph("\n").setFontSize(2));
    }

    private void addProcessTable(Document document, List<PowderCoatingProcess> processes) {
        // Add title for items section
        Paragraph itemsTitle = new Paragraph("Process Items")
                .setBold()
                .setFontSize(11)
                .setFontColor(SECONDARY_COLOR)
                .setMarginBottom(3);
        document.add(itemsTitle);
        
        // Create styled table
        Table table = new Table(new float[]{1, 4, 2, 2, 2, 2, 3})
                .useAllAvailableWidth()
                .setMarginTop(2);

        // Add headers with styling
        Stream.of("No.", "Particulars", "Total Quantity", "Total Bags", "Unit Price", "Total Amount", "Remarks")
            .forEach(title -> table.addHeaderCell(
                    new Cell().add(new Paragraph(title).setFontSize(9))
                            .setBackgroundColor(SECONDARY_COLOR)
                            .setFontColor(TEXT_LIGHT)
                            .setBold()
                            .setPadding(6)
            ));
        
        AtomicInteger rowNum = new AtomicInteger(1);
        AtomicInteger totalQuantity = new AtomicInteger(0);
        AtomicInteger totalBagsSum = new AtomicInteger(0);
        AtomicReference<BigDecimal> totalAmountSum = new AtomicReference<>(BigDecimal.ZERO);
        
        processes.forEach(process -> {
            // Fetch items for this process
            List<PowderCoatingProcessItem> items = processItemRepository.findByPowderCoatingProcessId(process.getId());
            
            // Iterate through all items of each process
            items.forEach(item -> {
                boolean isEvenRow = rowNum.get() % 2 == 0;
                Color rowColor = isEvenRow ? BACKGROUND_LIGHT : ColorConstants.WHITE;
                
                table.addCell(new Cell()
                        .add(new Paragraph(String.valueOf(rowNum.getAndIncrement())).setFontSize(9))
                        .setBackgroundColor(rowColor)
                        .setPadding(5));
                
                table.addCell(new Cell()
                        .add(new Paragraph(item.getProduct() != null ? item.getProduct().getName() : "N/A").setFontSize(9))
                        .setBackgroundColor(rowColor)
                        .setPadding(5));
                
                int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                int totalBags = item.getTotalBags() != null ? item.getTotalBags() : 0;
                BigDecimal totalAmount = item.getTotalAmount() != null ? item.getTotalAmount() : BigDecimal.ZERO;
                
                totalQuantity.addAndGet(quantity);
                totalBagsSum.addAndGet(totalBags);
                totalAmountSum.updateAndGet(sum -> sum.add(totalAmount));
                
                table.addCell(new Cell()
                        .add(new Paragraph(String.valueOf(quantity)).setFontSize(9))
                        .setBackgroundColor(rowColor)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(5));
                
                table.addCell(new Cell()
                        .add(new Paragraph(String.valueOf(totalBags)).setFontSize(9))
                        .setBackgroundColor(rowColor)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(5));
                
                table.addCell(new Cell()
                        .add(new Paragraph(item.getUnitPrice() != null ? item.getUnitPrice().toString() : "0.00").setFontSize(9))
                        .setBackgroundColor(rowColor)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(5));
                
                table.addCell(new Cell()
                        .add(new Paragraph(item.getTotalAmount() != null ? item.getTotalAmount().toString() : "0.00").setFontSize(9))
                        .setBackgroundColor(rowColor)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setPadding(5));
                
                table.addCell(new Cell()
                        .add(new Paragraph(item.getRemarks() != null ? item.getRemarks() : "").setFontSize(9))
                        .setBackgroundColor(rowColor)
                        .setPadding(5));
            });
        });
        
        // Add total row with proper styling
        table.addCell(new Cell()
                .add(new Paragraph("").setFontSize(9))
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(5));
        
        Cell totalLabelCell = new Cell()
                .add(new Paragraph("Total")
                        .setBold()
                        .setFontSize(9)
                        .setFontColor(PRIMARY_COLOR))
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(5);
        table.addCell(totalLabelCell);
        
        table.addCell(new Cell()
                .add(new Paragraph(String.valueOf(totalQuantity.get()))
                        .setBold()
                        .setFontSize(9)
                        .setFontColor(TEXT_DARK))
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(5));
        
        table.addCell(new Cell()
                .add(new Paragraph(String.valueOf(totalBagsSum.get()))
                        .setBold()
                        .setFontSize(9)
                        .setFontColor(TEXT_DARK))
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(5));
        
        table.addCell(new Cell()
                .add(new Paragraph("").setFontSize(9))
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(5));
        
        table.addCell(new Cell()
                .add(new Paragraph(totalAmountSum.get().toString())
                        .setBold()
                        .setFontSize(9)
                        .setFontColor(TEXT_DARK))
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(5));
        
        table.addCell(new Cell()
                .add(new Paragraph("").setFontSize(9))
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(5));
        
        document.add(table);
    }

    private void addTotal(Document document, List<PowderCoatingProcess> processes) {
        BigDecimal total = processes.stream()
            .flatMap(process -> processItemRepository.findByPowderCoatingProcessId(process.getId()).stream())
            .map(item -> item.getTotalAmount() != null ? item.getTotalAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Create summary table with styling
        Table summaryTable = new Table(2)
                .useAllAvailableWidth()
                .setMarginTop(5);
                
        summaryTable.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setWidth(350)); // Empty cell for spacing
                
        // Right side - totals with styling
        Cell totalsCell = new Cell();
        Table totalsTable = new Table(2).useAllAvailableWidth();
        
        // Grand total with prominent styling
        addTotalRow(totalsTable, "GRAND TOTAL", total.toString() + "/-", true);
        
        totalsCell.add(totalsTable)
                .setBorder(new SolidBorder(SECONDARY_COLOR, 1))
                .setBackgroundColor(BACKGROUND_LIGHT)
                .setPadding(8);
                
        summaryTable.addCell(new Cell().setBorder(Border.NO_BORDER)); // Empty cell
        summaryTable.addCell(totalsCell);
        
        document.add(summaryTable);
    }
    
    private void addTotalRow(Table table, String label, String value, boolean isGrandTotal) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label)
                        .setBold()
                        .setFontColor(isGrandTotal ? PRIMARY_COLOR : SECONDARY_COLOR))
                .setBorder(Border.NO_BORDER);
                
        Cell valueCell = new Cell()
                .add(new Paragraph(value)
                        .setBold()
                        .setFontColor(isGrandTotal ? PRIMARY_COLOR : TEXT_DARK))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);
                
        if (isGrandTotal) {
            // Add top border for grand total
            labelCell.setBorderTop(new SolidBorder(SECONDARY_COLOR, 1));
            valueCell.setBorderTop(new SolidBorder(SECONDARY_COLOR, 1));
            
            // Font size for grand total
            labelCell.setFontSize(11);
            valueCell.setFontSize(11);
        } else {
            labelCell.setFontSize(9);
            valueCell.setFontSize(9);
        }
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addSignature(Document document) {
        document.add(new Paragraph("\n").setFontSize(2));
        
        // Add signature line
        Paragraph signature = new Paragraph("Authorized Signatory")
                .setBold()
                .setFontSize(9)
                .setFontColor(SECONDARY_COLOR)
                .setTextAlignment(TextAlignment.RIGHT);
        document.add(signature);
    }

    private void addFooter(PdfDocument pdfDoc, Document document) {
        float footerY = 20;  // Distance from bottom
        float pageWidth = pdfDoc.getDefaultPageSize().getWidth();

        // Create styled footer with background
        Table footerBgTable = new Table(1)
                .useAllAvailableWidth()
                .setFixedPosition(36, footerY - 10, pageWidth - 72);
                
        Cell footerBgCell = new Cell()
                .setHeight(30)
                .setBackgroundColor(SECONDARY_COLOR)
                .setBorder(Border.NO_BORDER);
        footerBgTable.addCell(footerBgCell);

        // Create footer content table
        Table footerTable = new Table(2)
                .useAllAvailableWidth()
                .setFixedPosition(36, footerY, pageWidth - 72);

        // Center - Contact information
        Cell contactCell = new Cell()
                .add(new Paragraph("JK Industies [ CONTACT NO. 9979032430 ]")
                        .setFontSize(8)
                        .setFontColor(TEXT_LIGHT))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.LEFT);
                
        // Right - Website or additional info
        Cell websiteCell = new Cell()
                .add(new Paragraph("https://jkindustriesrajkot.com/")
                        .setFontSize(8)
                        .setFontColor(TEXT_LIGHT))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);

        footerTable.addCell(contactCell);
        footerTable.addCell(websiteCell);

        // Add both tables to document
        document.add(footerBgTable);
        document.add(footerTable);
    }
}
