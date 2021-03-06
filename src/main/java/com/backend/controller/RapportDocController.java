package com.backend.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;
import javax.validation.Valid;

import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.backend.message.response.ResponseMessage;
import com.backend.model.RapportDoc;
import com.backend.model.Analyse;
import com.backend.model.Rapport;
import com.backend.repository.RapportDocRepository;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPTableHeader;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/dms")
public class RapportDocController {
	
	@Autowired
	RapportDocRepository rapDocRepo;
	
	@GetMapping("/getAllRapports")
	@Secured({"ROLE_PL","ROLE_ADMIN"})
	public List<RapportDoc> getAllRapports() {
		System.out.println("Get all RapportDocs...");
	    List<RapportDoc> rapportDocs = new ArrayList<>();
	    rapDocRepo.findAll().forEach(rapportDocs::add);
	    return rapportDocs;
	}
	
	@PostMapping("/getByDate")
	@Secured({"ROLE_PATIENT"})
	public List<RapportDoc> getByDate(@Valid @RequestBody long idPatient, String dateCreation ) {
		System.out.println("Get RapportDocs By DateCreation...");
	    List<RapportDoc> rapportDocs = new ArrayList<>();
	    rapDocRepo.findByDateCreationAndIdPatient(dateCreation, idPatient).forEach(rapportDocs::add);
	    return rapportDocs;
	}
	
	@GetMapping("/getById/{id}")
	@Secured({"ROLE_PATIENT"})
	public List<RapportDoc> getById(@PathVariable("id") long idPatient) {
		System.out.println("Get all RapportDocs...");
	    List<RapportDoc> rapportDocs = new ArrayList<>();
	    rapDocRepo.findByIdPatient(idPatient).forEach(rapportDocs::add);
	    return rapportDocs;
	}
	
	@PostMapping("/getByFullname")
	@Secured({"ROLE_PL","ROLE_ADMIN"})
	public List<RapportDoc> getByFullname(@Valid @RequestBody String fullnamePatient) {
		System.out.println("Get all RapportDocs...");
	    List<RapportDoc> rapportDocs = new ArrayList<>();
	    rapDocRepo.findByfullnamePatient(fullnamePatient).forEach(rapportDocs::add);
	    return rapportDocs;
	}
	
	@DeleteMapping("/{id}")
	@Secured({"ROLE_ADMIN"})
	public ResponseEntity<?> deleteRapport(@PathVariable("id") long idRapport) {
	    System.out.println("Delete Rapport with ID = " + idRapport + "...");
	    rapDocRepo.deleteById(idRapport);
	    return new ResponseEntity<>(new ResponseMessage("The Document has been deleted!"), HttpStatus.OK);
	}
	
	@PostMapping("/generateDoc")
	@Secured({"ROLE_PL","ROLE_ADMIN"})
	public ResponseEntity<?> generateDoc(@RequestBody Rapport rapport) throws IOException {
		Document document = new Document();
		System.out.println(rapport);
		String fileName = "Rapport_" + rapport.getNamePatient().toString() + "_" + rapport.getIdRapport().toString();
		try {
			
			PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(fileName+".pdf"));
			document.open();
			
			//Add Image, Scale to new height and new width of image
			Image image = Image.getInstance(".\\src\\main\\resources\\static\\logo.png");
			image.scaleAbsolute(50, 50);
		    document.add(image);
		    
		    // add a couple of blank lines
		    document.add(new Paragraph(new Phrase(Chunk.NEWLINE)));
		    
		    // add Paragraph as Title
		    Font fontTitle = FontFactory.getFont(FontFactory.TIMES_ROMAN, 16, BaseColor.BLUE);
		    Chunk titleChunk = new Chunk("RÉSULTATS DES ANALYSES", fontTitle);
		    Paragraph title = new Paragraph(new Phrase(titleChunk));
		    title.setAlignment(Element.ALIGN_CENTER);
			document.add(title);
			
			// add a couple of blank lines
		    document.add(new Paragraph(new Phrase(Chunk.NEWLINE)));
			
		    // creating Table for information about the report and patient
			PdfPTable tableInfo = new PdfPTable(2);
			tableInfo.setWidthPercentage(100); //Width 100%
			tableInfo.setSpacingBefore(10f); //Space before table
			tableInfo.setSpacingAfter(10f); //Space after table
			PdfPCell cell1 = new PdfPCell(new Paragraph("EFFECTUEES LE : "+rapport.getDateAnalyse().toString()));
			cell1.setFixedHeight(30f);
			PdfPCell cell2 = new PdfPCell(new Paragraph("DOSSIER N° : "+rapport.getIdRapport().toString()));
			cell2.setFixedHeight(30f);
			PdfPCell cell3 = new PdfPCell(new Paragraph("POUR : "+rapport.getNamePatient().toString()));
			cell3.setFixedHeight(30f);
			PdfPCell cell4 = new PdfPCell(new Paragraph("DOCTEUR : "+rapport.getNomDocteur().toString()));
			cell4.setFixedHeight(30f);
			tableInfo.addCell(cell1);
			tableInfo.addCell(cell2);
			tableInfo.addCell(cell3);
			tableInfo.addCell(cell4);
	        document.add(tableInfo);
			
	        document.add(new Paragraph(new Phrase(Chunk.NEWLINE)));
	        
	    	// creating Table for Analyses
	        PdfPTable tableAnalyse = new PdfPTable(3);
	        tableAnalyse.setWidthPercentage(100); //Width 100%
	        tableAnalyse.setSpacingBefore(10f); //Space before table
	        tableAnalyse.setSpacingAfter(10f); //Space after table
	        tableAnalyse.addCell("Analyse Demandé");
	        tableAnalyse.addCell("Résultat");
	        tableAnalyse.addCell("Valeur Normal");
	        tableAnalyse.setHeaderRows(1);
	        rapport.getAnalyseTab().forEach(item -> {
	        	PdfPCell cel1 = new PdfPCell(new Paragraph(item.getAnalyseDemande()));
	        	PdfPCell cel2 = new PdfPCell(new Paragraph(item.getResultat()));
	        	PdfPCell cel3 = new PdfPCell(new Paragraph(item.getValNormal()));
	        	tableAnalyse.addCell(cel1);
	        	tableAnalyse.addCell(cel2);
	        	tableAnalyse.addCell(cel3);
	        });
	        document.add(tableAnalyse);
			document.close();
	        writer.close();
	        
	        String fileN = StringUtils.cleanPath(fileName+".pdf");
			File file = new File(fileN);
			byte[] bytesArray = new byte[(int) file.length()];
			FileInputStream fis = new FileInputStream(file);
			fis.read(bytesArray); //read file into bytes[]
			fis.close();
			
			Properties prop = new Properties();
			prop.put("mail.smtp.auth", true);
			prop.put("mail.smtp.starttls.enable", "true");
			prop.put("mail.smtp.host", "smtp.gmail.com");
			prop.put("mail.smtp.port", "25");
			String mail = "abdoujojo95@gmail.com";
			String pass = "hgfdsqpoiuytreza";
			
			Session session = Session.getInstance(prop, new Authenticator() {
			    @Override
			    protected PasswordAuthentication getPasswordAuthentication() {
			        return new PasswordAuthentication(mail, pass);
			    }
			});
			
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(mail));
			message.setRecipients(
			  Message.RecipientType.TO, InternetAddress.parse(rapport.getMailDocteur().toString()));
			message.setSubject("Les Analyses concernant le patient "+rapport.getNamePatient().toString());
			String msg = "Bonjour,\n"
						+"Vous trouveriez ci-joint le rapport d\'analayse du patient "+rapport.getNamePatient().toString()+".\n"
						+"Cordialement,";
			BodyPart mimeBodyPart = new MimeBodyPart();
			mimeBodyPart.setText(msg);
			MimeBodyPart attachmentBodyPart = new MimeBodyPart();
			attachmentBodyPart.attachFile(file);
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(mimeBodyPart);
			multipart.addBodyPart(attachmentBodyPart);
			message.setContent(multipart);
			Transport.send(message);
			
	        RapportDoc doc = new RapportDoc(rapport.getIdRapport(), rapport.getIdPatient(), rapport.getNamePatient(), fileN,
	        		".pdf", bytesArray, rapport.getDateAnalyse());
	        rapDocRepo.save(doc);
		} catch (DocumentException | MessagingException | IOException e) {
			e.printStackTrace();
	    }
		return new ResponseEntity<>(new ResponseMessage("PDF has been created"), HttpStatus.OK);
	}
	
	@GetMapping("/download/{id}")
	public ResponseEntity<?> download(@PathVariable("id") long idRapport) {
	    List<RapportDoc> rapportDocs = new ArrayList<>();
	    RapportDoc docInfo = rapDocRepo.findByIdRapport(idRapport);
	    return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/pdf"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + docInfo.getNameFile() + "\"")
                .body(new ByteArrayResource(docInfo.getFileData()));
	}

}
