pandoc -f markdown -t html5 DEVELOPER_GUIDE.md -o DEVELOPER_GUIDE.html --lua-filter=links-to-html.lua
#pandoc -V geometry:a4paper --pdf-engine=xelatex -V "mainfont:DejaVu Sans" -V "monofont:DejaVu Sans Mono" DEVELOPER_GUIDE.md -o DEVELOPER_GUIDE.pdf
