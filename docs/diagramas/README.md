# Diagramas do documento da APS

Fontes dos diagramas usados no documento entregue. As imagens (`.png`, usadas no documento, e `.svg`) são geradas a partir delas pelos comandos da seção seguinte e não são versionadas; toda alteração em um diagrama deve ser feita no arquivo-fonte, com a imagem gerada de novo.

| Figura | Item | Fonte | Autor | Revisor |
|---|---|---|---|---|
| 1.1 – Visão geral do funcionamento de uma aplicação Web | 1.1 | `fig-1.1-visao-geral-web.dot` | Claudio Cristiano | Claudio Eliziario |
| 2.1 – Diagrama de componentes | 2.2 | `fig-2.1-componentes.dot` | Claudio Cristiano | Pedro Augusto |
| 2.2 – Diagrama de implantação | 2.2 | `fig-2.2-implantacao.dot` | Claudio Cristiano | Pedro Augusto |
| 2.3 – Diagrama de classes | 2.3 | `fig-2.3-classes.dot` | Claudio Cristiano | Pedro Augusto |
| 2.4 – Diagrama de atividades da decisão de autenticação | 2.3 | `fig-2.4-atividades-decisao.dot` | Claudio Cristiano | Pedro Augusto |
| 2.5 – Sequência da autenticação bem-sucedida | 2.4 | `fig-2.5-sequencia-sucesso.mmd` | Claudio Cristiano | Pedro Augusto |
| 2.6 – Sequência das falhas de autenticação | 2.4 | `fig-2.6-sequencia-falhas.mmd` | Claudio Cristiano | Pedro Augusto |

## Gerar as imagens

Os arquivos `.dot` usam o [Graphviz](https://graphviz.org/) e os `.mmd` usam o [Mermaid CLI](https://github.com/mermaid-js/mermaid-cli). Execute os comandos dentro desta pasta:

```bash
# Graphviz
for f in *.dot; do
  b=$(basename "$f" .dot)
  dot -Tsvg "$f" -o "$b.svg"
  dot -Tpng -Gdpi=200 "$f" -o "$b.png"
done

# Mermaid (npm install -g @mermaid-js/mermaid-cli)
for f in *.mmd; do
  b=$(basename "$f" .mmd)
  mmdc -i "$f" -o "$b.svg" -c mmd-config.json -C mmd.css -b white
  mmdc -i "$f" -o "$b.png" -c mmd-config.json -C mmd.css -b white -s 2
done
```

`mmd-config.json` e `mmd.css` fixam a fonte (DejaVu Sans) para que os diagramas de sequência fiquem com a mesma tipografia dos demais.
