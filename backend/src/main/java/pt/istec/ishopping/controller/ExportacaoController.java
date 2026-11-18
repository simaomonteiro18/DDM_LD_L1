package pt.istec.ishopping.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.istec.ishopping.service.ExportacaoCsvService;

@RestController
@RequestMapping("/api/compras")
public class ExportacaoController {

    // BOM do UTF-8, para o Excel reconhecer os acentos
    private static final String BOM = "﻿";
    private static final MediaType TEXTO_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ExportacaoCsvService exportacaoCsvService;

    public ExportacaoController(ExportacaoCsvService exportacaoCsvService) {
        this.exportacaoCsvService = exportacaoCsvService;
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar() {
        byte[] conteudo = (BOM + exportacaoCsvService.comprasFechadas()).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(TEXTO_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("compras-fechadas.csv").build().toString())
                .body(conteudo);
    }
}
