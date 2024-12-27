package com.inventory.service;

import com.inventory.dto.PowderCoatingProcessPdfDto;
import com.inventory.entity.Customer;
import com.inventory.entity.PowderCoatingProcess;
import com.inventory.exception.ValidationException;
import com.inventory.repository.CustomerRepository;
import com.inventory.repository.PowderCoatingProcessRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class PdfGenerationService {
    private final PowderCoatingProcessRepository processRepository;
    private final CustomerRepository customerRepository;

    public byte[] generateEstimatePdf(PowderCoatingProcessPdfDto dto) {
        try {
            Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ValidationException("Customer not found"));

            List<PowderCoatingProcess> processes = processRepository.findAllById(dto.getProcessIds());
            
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, out);
            
            document.open();
            addHeader(document, "J.K INDUSTRIES ESTIMATE");
            addCustomerDetails(document, customer);
            addProcessTable(document, processes);
            addTotal(document, processes);
            document.close();
            
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to generate PDF: " + e.getMessage());
        }
    }

    private void addHeader(Document document, String title) throws DocumentException {
        Font titleFont = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL);
        
        // Add title
        Paragraph header = new Paragraph(title, titleFont);
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);
        document.add(Chunk.NEWLINE);
        
        // Add date
        Paragraph date = new Paragraph("Date: " + 
            java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")), 
            normalFont);
        date.setAlignment(Element.ALIGN_RIGHT);
        document.add(date);
        document.add(Chunk.NEWLINE);
    }

    private void addCustomerDetails(Document document, Customer customer) throws DocumentException {
        Font boldFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL);

        // Customer basic details
        document.add(new Paragraph("To,", normalFont));
        document.add(new Paragraph(customer.getName(), boldFont));
        
        // Address
        if (customer.getAddress() != null) {
            document.add(new Paragraph(customer.getAddress(), normalFont));
        }
        
        // GST Number
        if (customer.getGst() != null) {
            document.add(new Paragraph("GST No: " + customer.getGst(), normalFont));
        }
        
        // Mobile
        if (customer.getMobile() != null) {
            document.add(new Paragraph("Mobile: " + customer.getMobile(), normalFont));
        }
        
        document.add(Chunk.NEWLINE);
    }

    private void addProcessTable(Document document, List<PowderCoatingProcess> processes) throws DocumentException {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        
        // Set relative column widths
        float[] columnWidths = new float[]{5f, 40f, 12f, 15f, 13f, 15f};
        table.setWidths(columnWidths);
        
        // Add headers with styling
        Stream.of("No.", "Particulars", "Total Quantity", "Remaining Quantity", "Unit Price", "Total Amount")
            .forEach(header -> {
                PdfPCell cell = new PdfPCell(new Phrase(header, new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            });
        
        // Add data with null checks
        AtomicInteger rowNum = new AtomicInteger(1);
        AtomicInteger totalQuantity = new AtomicInteger(0);
        AtomicInteger totalRemaining = new AtomicInteger(0);
        
        processes.forEach(process -> {
            // Serial number - center aligned
            PdfPCell noCell = new PdfPCell(new Phrase(String.valueOf(rowNum.getAndIncrement())));
            noCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(noCell);
            
            // Particulars - left aligned
            PdfPCell particularCell = new PdfPCell(new Phrase(process.getProduct() != null ? process.getProduct().getName() : "N/A"));
            particularCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(particularCell);
            
            int quantity = process.getQuantity() != null ? process.getQuantity() : 0;
            int remaining = process.getRemainingQuantity() != null ? process.getRemainingQuantity() : 0;
            
            totalQuantity.addAndGet(quantity);
            totalRemaining.addAndGet(remaining);
            
            // Numbers - right aligned
            PdfPCell quantityCell = new PdfPCell(new Phrase(String.valueOf(quantity)));
            quantityCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(quantityCell);
            
            PdfPCell remainingCell = new PdfPCell(new Phrase(String.valueOf(remaining)));
            remainingCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(remainingCell);
            
            PdfPCell priceCell = new PdfPCell(new Phrase(process.getUnitPrice() != null ? process.getUnitPrice().toString() : "0.00"));
            priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(priceCell);
            
            PdfPCell amountCell = new PdfPCell(new Phrase(process.getTotalAmount() != null ? process.getTotalAmount().toString() : "0.00"));
            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(amountCell);
        });
        
        // Add totals row with styling
        Font boldFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
        table.addCell("");
        
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("Total", boldFont));
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(totalLabelCell);
        
        PdfPCell totalQuantityCell = new PdfPCell(new Phrase(String.valueOf(totalQuantity.get()), boldFont));
        totalQuantityCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalQuantityCell);
        
        PdfPCell totalRemainingCell = new PdfPCell(new Phrase(String.valueOf(totalRemaining.get()), boldFont));
        totalRemainingCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalRemainingCell);
        
        table.addCell("");
        table.addCell("");
        
        document.add(table);
    }

    private void addTotal(Document document, List<PowderCoatingProcess> processes) throws DocumentException {
        Font boldFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
        
        BigDecimal total = processes.stream()
            .map(process -> process.getTotalAmount() != null ? process.getTotalAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        document.add(Chunk.NEWLINE);
        
        // Add summary
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(50);
        summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        
        summaryTable.addCell(new PdfPCell(new Phrase("Grand Total:", boldFont)));
        summaryTable.addCell(new PdfPCell(new Phrase(total.toString(), boldFont)));
        
        document.add(summaryTable);
        document.add(Chunk.NEWLINE);
        document.add(Chunk.NEWLINE);
        
        // Add signature line
        Paragraph signature = new Paragraph("Authorized Signatory", boldFont);
        signature.setAlignment(Element.ALIGN_RIGHT);
        document.add(signature);
    }
} 