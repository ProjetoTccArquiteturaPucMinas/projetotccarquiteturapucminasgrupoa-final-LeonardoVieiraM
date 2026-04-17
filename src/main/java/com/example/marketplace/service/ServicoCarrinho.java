package com.example.marketplace.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.marketplace.model.ItemCarrinho;
import com.example.marketplace.model.Produto;
import com.example.marketplace.model.ResumoCarrinho;
import com.example.marketplace.model.SelecaoCarrinho;
import com.example.marketplace.model.CategoriaProduto;
import com.example.marketplace.repository.ProdutoRepository;

@Service
public class ServicoCarrinho {

    private final ProdutoRepository repositorioProdutos;

    public ServicoCarrinho(ProdutoRepository repositorioProdutos) {
        this.repositorioProdutos = repositorioProdutos;
    }

    public ResumoCarrinho construirResumo(List<SelecaoCarrinho> selecoes) {
        List<ItemCarrinho> itens = new ArrayList<>();

        for (SelecaoCarrinho selecao : selecoes) {
            Produto produto = repositorioProdutos.buscarPorId(selecao.getProdutoId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + selecao.getProdutoId()));

            itens.add(new ItemCarrinho(produto, selecao.getQuantidade()));
        }

        // 1. Calcular Subtotal
        BigDecimal subtotal = itens.stream()
                .map(ItemCarrinho::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Calcular Desconto por Quantidade Total de Itens
        int totalItens = itens.stream().mapToInt(ItemCarrinho::getQuantidade).sum();
        double descQuantidade = 0;
        
        if (totalItens >= 4) descQuantidade = 10.0;
        else if (totalItens == 3) descQuantidade = 7.0;
        else if (totalItens == 2) descQuantidade = 5.0;

        // 3. Calcular Desconto por Categoria (Cumulativo por item)
        double descCategoriaAcumulado = 0;
        for (ItemCarrinho item : itens) {
            double percentualCategoria = 0;
            CategoriaProduto categoria = item.getProduto().getCategoria();
            
            // Comparação direta com o ENUM (sem toUpperCase)
            switch (categoria) {
                case CAPINHA: percentualCategoria = 3.0; break;
                case CARREGADOR: percentualCategoria = 5.0; break;
                case FONE: percentualCategoria = 3.0; break;
                case PELICULA: 
                case SUPORTE: percentualCategoria = 2.0; break;
            }
            descCategoriaAcumulado += (percentualCategoria * item.getQuantidade());
        }

        // 4. Regra de Desconto Máximo (25%)
        double percentualSoma = descQuantidade + descCategoriaAcumulado;
        BigDecimal percentualFinal = BigDecimal.valueOf(Math.min(percentualSoma, 25.0));

        // 5. Cálculos de Valores
        BigDecimal valorDesconto = subtotal.multiply(percentualFinal)
                                           .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        BigDecimal totalFinal = subtotal.subtract(valorDesconto);

        return new ResumoCarrinho(itens, subtotal, percentualFinal, valorDesconto, totalFinal);
    }
}
