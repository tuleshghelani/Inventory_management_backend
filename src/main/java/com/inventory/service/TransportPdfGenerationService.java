package com.inventory.service;

import com.inventory.dao.TransportDao;
import com.inventory.dto.TransportPdfDto;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
public class TransportPdfGenerationService {
    // Color scheme as per requirements
    private static final Color PRIMARY_COLOR = new DeviceRgb(245, 106, 73);     // #f56a49
    private static final Color SECONDARY_COLOR = new DeviceRgb(0, 63, 105);     // #003f69
    private static final Color PRIMARY_LIGHT = new DeviceRgb(255, 139, 115);    // #ff8b73
    private static final Color SECONDARY_LIGHT = new DeviceRgb(0, 92, 158);     // #005c9e
    private static final Color TEXT_DARK = new DeviceRgb(51, 51, 51);           // #333333
    private static final Color TEXT_LIGHT = new DeviceRgb(255, 255, 255);       // #ffffff
    private static final Color BACKGROUND_LIGHT = new DeviceRgb(245, 245, 245); // #f5f5f5

    private final TransportDao transportDao;
    private final UtilityService utilityService;

    public byte[] generateTransportPdf(TransportPdfDto dto) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Map<String, Object> transportData = transportDao.getTransportPdfData(dto.getId(), currentUser.getClient().getId());

            try (PdfDocument pdf = new PdfDocument(new PdfWriter(outputStream))) {
                Document document = new Document(pdf, PageSize.A4);
                document.setMargins(36, 36, 36, 36);

                // Get date from transport data if available, otherwise use current date
                java.time.OffsetDateTime transportDate;
                Object createdAtObj = transportData.get("createdAt");
                if (createdAtObj != null) {
                    if (createdAtObj instanceof java.time.OffsetDateTime) {
                        transportDate = (java.time.OffsetDateTime) createdAtObj;
                    } else if (createdAtObj instanceof java.time.Instant) {
                        transportDate = ((java.time.Instant) createdAtObj).atOffset(java.time.ZoneOffset.UTC);
                    } else if (createdAtObj instanceof java.time.LocalDateTime) {
                        transportDate = ((java.time.LocalDateTime) createdAtObj).atOffset(java.time.ZoneOffset.UTC);
                    } else {
                        transportDate = java.time.OffsetDateTime.now();
                    }
                } else {
                    transportDate = java.time.OffsetDateTime.now();
                }

                // Add content
                addHeader(document, transportDate);
                addCustomerDetails(document, transportData, transportDate);
                addTransportDetails(document, transportData);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> bags = (List<Map<String, Object>>) transportData.get("bags");
                addBagsTable(document, bags);
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

    private void addHeader(Document document, java.time.OffsetDateTime transportDate) {
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
        rightDetailsCell.add(new Paragraph("GST NO.24AAMFJ9388A1Z4")
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

        // Add styled transport heading
        Table transportHeadingTable = new Table(1).useAllAvailableWidth();
        Cell transportHeadingCell = new Cell()
                .add(new Paragraph("J.K INDUSTRIES TRANSPORT")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(12)
                        .setBold()
                        .setFontColor(TEXT_LIGHT))
                .setBackgroundColor(PRIMARY_COLOR)
                .setPadding(8);
        transportHeadingTable.addCell(transportHeadingCell);
        
        document.add(new Paragraph("\n").setFontSize(2));
        document.add(transportHeadingTable);
        document.add(new Paragraph("\n").setFontSize(2));
    }

    private void addCustomerDetails(Document document, Map<String, Object> data, java.time.OffsetDateTime transportDate) {
        // Create a 2-column table for customer details
        Table infoTable = new Table(new float[]{1, 1}).useAllAvailableWidth();
        
        // Left side - Customer information
        Cell leftCell = new Cell();
        String customerName = data.get("customerName") != null ? data.get("customerName").toString() : "";
        Paragraph customerNamePara = new Paragraph(customerName)
                .setBold()
                .setFontSize(9)
                .setFontColor(PRIMARY_COLOR);
        
        leftCell.add(new Paragraph("To,").setFontSize(8).setFontColor(TEXT_DARK))
                .add(customerNamePara);
        
        // Address on first line
        if (data.get("customerAddress") != null) {
            leftCell.add(new Paragraph(data.get("customerAddress").toString()).setFontSize(8).setFontColor(TEXT_DARK));
        }
        
        // Mobile number on new line
        if (data.get("customerMobile") != null) {
            leftCell.add(new Paragraph("Mo.No. " + data.get("customerMobile").toString()).setFontSize(8).setFontColor(TEXT_DARK));
        }
        
        leftCell.setBackgroundColor(BACKGROUND_LIGHT)
                .setBorder(new SolidBorder(SECONDARY_LIGHT, 1))
                .setBorderRight(Border.NO_BORDER)
                .setPadding(8)
                .setTextAlignment(TextAlignment.LEFT);
        
        // Right side - GST and Date (left-aligned)
        Cell rightCell = new Cell();
        String dateStr = transportDate.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        
        // GST number
        if (data.get("customerGst") != null) {
            rightCell.add(new Paragraph("GST No: " + data.get("customerGst").toString()).setFontSize(8).setFontColor(TEXT_DARK));
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

    private void addTransportDetails(Document document, Map<String, Object> data) {
        // Add title for transport details section
        Paragraph detailsTitle = new Paragraph("Transport Details")
                .setBold()
                .setFontSize(9)
                .setFontColor(SECONDARY_COLOR)
                .setMarginBottom(2);
        document.add(detailsTitle);
        
        // Create styled table
        Table table = new Table(new float[]{1, 1})
                .useAllAvailableWidth()
                .setMarginTop(2);
        
        // Add styled rows
        addDetailRow(table, "Total Bags", data.get("totalBags") != null ? data.get("totalBags").toString() : "0", false);
        addDetailRow(table, "Total Weight", data.get("totalWeight") != null ? data.get("totalWeight").toString() + " kg" : "0 kg", false);
        
        document.add(table);
        document.add(new Paragraph("\n").setFontSize(2));
    }

    private void addBagsTable(Document document, List<Map<String, Object>> bags) {
        if (bags == null || bags.isEmpty()) {
            return;
        }
        
        // Add title for bags section
        Paragraph bagsTitle = new Paragraph("Bags & Items")
                .setBold()
                .setFontSize(11)
                .setFontColor(SECONDARY_COLOR)
                .setMarginBottom(5);
        document.add(bagsTitle);
        
        AtomicInteger currentBagNumber = new AtomicInteger(1);

        for (int bagIndex = 0; bagIndex < bags.size(); bagIndex++) {
            Map<String, Object> bag = bags.get(bagIndex);
            
            // Calculate bag number range
            int numberOfBags = bag.get("numberOfBags") != null 
                ? ((Number) bag.get("numberOfBags")).intValue() 
                : 1;
            String bagNumberText = numberOfBags > 1 
                ? "Bag #" + currentBagNumber.get() + "-" + (currentBagNumber.get() + numberOfBags - 1)
                : "Bag #" + currentBagNumber.get();
            
            // Bag number and Weight in one row
            Table bagHeaderTable = new Table(new float[]{1, 1}).useAllAvailableWidth();
            bagHeaderTable.setMarginTop(bagIndex > 0 ? 10 : 3);
            
            // Left side - Bag number
            Cell bagTitleCell = new Cell()
                    .add(new Paragraph(bagNumberText)
                            .setBold()
                            .setFontSize(10)
                            .setFontColor(SECONDARY_COLOR))
                    .setBorder(Border.NO_BORDER)
                    .setPadding(5)
                    .setTextAlignment(TextAlignment.LEFT);
            bagHeaderTable.addCell(bagTitleCell);
            
            // Right side - Weight
            Cell weightCell = new Cell();
            if (bag.get("weight") != null) {
                weightCell.add(new Paragraph("Weight: " + bag.get("weight").toString() + " kg")
                        .setBold()
                        .setFontSize(9)
                        .setFontColor(PRIMARY_COLOR));
            }
            weightCell.setBorder(Border.NO_BORDER)
                    .setPadding(5)
                    .setTextAlignment(TextAlignment.RIGHT);
            bagHeaderTable.addCell(weightCell);
            
            document.add(bagHeaderTable);

            // Items table with improved styling
            Table itemsTable = new Table(new float[]{4, 2, 3})
                    .useAllAvailableWidth()
                    .setMarginTop(2);

            // Add headers with styling
            Stream.of("Product Name", "Quantity", "Remarks")
                .forEach(header -> itemsTable.addHeaderCell(
                        new Cell().add(new Paragraph(header).setFontSize(9))
                                .setBackgroundColor(SECONDARY_COLOR)
                                .setFontColor(TEXT_LIGHT)
                                .setBold()
                                .setPadding(6)
                ));

            // Add items
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) bag.get("items");
            if (items != null && !items.isEmpty()) {
                AtomicInteger rowNum = new AtomicInteger(0);
                for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                    Map<String, Object> item = items.get(itemIndex);
                    boolean isEvenRow = rowNum.getAndIncrement() % 2 == 0;
                    Color rowColor = isEvenRow ? BACKGROUND_LIGHT : ColorConstants.WHITE;
                    
                    itemsTable.addCell(new Cell()
                            .add(new Paragraph(item.get("productName") != null ? item.get("productName").toString() : "N/A").setFontSize(9))
                            .setBackgroundColor(rowColor)
                            .setPadding(5));
                    
                    // Calculate quantity per bag
                    Object quantityObj = item.get("quantity");
                    Object numberOfBagsObj = bag.get("numberOfBags");
                    String quantityDisplay;
                    
                    if (numberOfBagsObj != null && ((Number) numberOfBagsObj).intValue() > 0) {
                        double quantity = quantityObj != null ? ((Number) quantityObj).doubleValue() : 0.0;
                        int tempNumberOfBags = ((Number) numberOfBagsObj).intValue();
                        quantityDisplay = String.format("%.2f", quantity / tempNumberOfBags);
                    } else {
                        quantityDisplay = quantityObj != null ? quantityObj.toString() : "0";
                    }
                    
                    itemsTable.addCell(new Cell()
                            .add(new Paragraph(quantityDisplay).setFontSize(9))
                            .setBackgroundColor(rowColor)
                            .setTextAlignment(TextAlignment.RIGHT)
                            .setPadding(5));
                    
                    itemsTable.addCell(new Cell()
                            .add(new Paragraph(item.get("remarks") != null ? item.get("remarks").toString() : "").setFontSize(9))
                            .setBackgroundColor(rowColor)
                            .setPadding(5));
                }
            } else {
                // Empty state - add a single row indicating no items
                Cell emptyCell1 = new Cell()
                        .add(new Paragraph("No items in this bag").setFontSize(9).setFontColor(TEXT_DARK))
                        .setBackgroundColor(BACKGROUND_LIGHT)
                        .setPadding(8)
                        .setTextAlignment(TextAlignment.CENTER);
                Cell emptyCell2 = new Cell()
                        .setBackgroundColor(BACKGROUND_LIGHT)
                        .setPadding(8);
                Cell emptyCell3 = new Cell()
                        .setBackgroundColor(BACKGROUND_LIGHT)
                        .setPadding(8);
                itemsTable.addCell(emptyCell1);
                itemsTable.addCell(emptyCell2);
                itemsTable.addCell(emptyCell3);
            }

            document.add(itemsTable);
            document.add(new Paragraph("\n").setFontSize(2));

            currentBagNumber.addAndGet(numberOfBags);
        }
    }

    @SuppressWarnings("unchecked")
    private void addDetailRow(Table table, String label, String value, boolean isBold) {
        Paragraph labelPara = new Paragraph(label + ":")
                .setBold()
                .setFontSize(isBold ? 9 : 8)
                .setFontColor(SECONDARY_COLOR);
        
        Paragraph valuePara = new Paragraph(value)
                .setFontSize(isBold ? 9 : 8)
                .setFontColor(TEXT_DARK);
        if (isBold) {
            valuePara.setBold();
        }
        
        Cell labelCell = new Cell()
                .add(labelPara)
                .setBorder(Border.NO_BORDER);
        
        Cell valueCell = new Cell()
                .add(valuePara)
                .setBorder(Border.NO_BORDER);
        
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
