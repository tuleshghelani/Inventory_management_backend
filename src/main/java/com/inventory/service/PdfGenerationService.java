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
    private final PowderCoatingProcessItemRepository processItemRepository;
    private final CustomerRepository customerRepository;
    private final UtilityService utilityService;

    public byte[] generateEstimatePdf(PowderCoatingProcessPdfDto dto) {
        try {
            Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ValidationException("Customer not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(customer.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to view this customer");
            }


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
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        
        float[] columnWidths = new float[]{5f, 35f, 12f, 12f, 12f, 12f, 12f};
        table.setWidths(columnWidths);
        
        Stream.of("No.", "Particulars", "Total Quantity", "Total Bags", "Unit Price", "Total Amount", "Remarks")
            .forEach(header -> {
                PdfPCell cell = new PdfPCell(new Phrase(header, new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            });
        
        AtomicInteger rowNum = new AtomicInteger(1);
        AtomicInteger totalQuantity = new AtomicInteger(0);
        AtomicInteger totalBagsSum = new AtomicInteger(0);
        
        processes.forEach(process -> {
            // Fetch items for this process
            List<PowderCoatingProcessItem> items = processItemRepository.findByPowderCoatingProcessId(process.getId());
            
            // Iterate through all items of each process
            items.forEach(item -> {
                PdfPCell noCell = new PdfPCell(new Phrase(String.valueOf(rowNum.getAndIncrement())));
                noCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(noCell);
                
                PdfPCell particularCell = new PdfPCell(new Phrase(item.getProduct() != null ? item.getProduct().getName() : "N/A"));
                particularCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(particularCell);
                
                int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                int totalBags = item.getTotalBags() != null ? item.getTotalBags() : 0;
                
                totalQuantity.addAndGet(quantity);
                totalBagsSum.addAndGet(totalBags);
                
                PdfPCell quantityCell = new PdfPCell(new Phrase(String.valueOf(quantity)));
                quantityCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(quantityCell);
                
                PdfPCell bagsCell = new PdfPCell(new Phrase(String.valueOf(totalBags)));
                bagsCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(bagsCell);
                
                PdfPCell priceCell = new PdfPCell(new Phrase(item.getUnitPrice() != null ? item.getUnitPrice().toString() : "0.00"));
                priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(priceCell);
                
                PdfPCell amountCell = new PdfPCell(new Phrase(item.getTotalAmount() != null ? item.getTotalAmount().toString() : "0.00"));
                amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(amountCell);
                
                PdfPCell remarksCell = new PdfPCell(new Phrase(item.getRemarks() != null ? item.getRemarks() : ""));
                remarksCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(remarksCell);
            });
        });
        
        Font boldFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
        table.addCell("");
        
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("Total", boldFont));
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(totalLabelCell);
        
        PdfPCell totalQuantityCell = new PdfPCell(new Phrase(String.valueOf(totalQuantity.get()), boldFont));
        totalQuantityCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalQuantityCell);
        
        PdfPCell totalBagsCell = new PdfPCell(new Phrase(String.valueOf(totalBagsSum.get()), boldFont));
        totalBagsCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalBagsCell);
        
        table.addCell("");
        table.addCell("");
        table.addCell("");
        
        document.add(table);
    }

    private void addTotal(Document document, List<PowderCoatingProcess> processes) throws DocumentException {
        Font boldFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
        
        BigDecimal total = processes.stream()
            .flatMap(process -> processItemRepository.findByPowderCoatingProcessId(process.getId()).stream())
            .map(item -> item.getTotalAmount() != null ? item.getTotalAmount() : BigDecimal.ZERO)
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