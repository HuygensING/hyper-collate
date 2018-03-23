# Indien mogelijk (qua tijd) graag vier hypergraphs: variation-in-structure-1, variation-in-structure-2, variation-in-order-1, variation-in-order-2.

# In de bijlage alle XML files van de witness samples.

export hc=http://localhost:63458/collations
for x in dot svg png; do
	# Variation in structure: 

	# (1) Within one witness: Woolf's Initial Holograph Draft p.157 (witness idl "IHD-157-sample")
  wget ${hc}/variation-in-structure/witnesses/IHD.$x -O variation-in-structure-1.$x
  
	# (2) Between witnesses: Woolf's Initial Holograph Draft p.157 (witness id "IHD-157-sample") + Woolf's Typescript p. 5 (witness id "TS-5-sample")
  wget ${hc}/variation-in-structure.$x -O variation-in-structure-2.$x

	# NOOT: Ik contrasteer de hypergraph output met de ASCII visualisatie en Variant Graph (VG; beiden gegenereerd met HyperCollate) van een ander sample
	#  met structurele variatie om te illustreren wat de voor- en nadelen zijn van bepaalde visualisaties. Kort gezegd zie je bij de ASCII tabel niet duidelijk
	# dat er verschil in structuur is, maar bepaalde synoniemen worden gealigned omdat ze op dezelfde relatieve positie staan.
	# In de VG zie je bijvoorbeeld wel dat deze tokens geen match zijn.

	# Variation in order

	# (1) Within one witness  (open variant / simultaneity): Woolf's Initial Holograph Draft p.161 (witness id "IHD-161-sample")
  wget ${hc}/variation-in-order-1/witnesses/IHD.$x -O variation-in-order-1.$x

	# (2) Between witnesses: Woolf's Initial Holograph Draft p.155 (witness id "IHD-155-sample") + Woolf's Typescript p.4 (witness id "TS-4-sample")
  wget ${hc}/variation-in-order-2.$x -O variation-in-order-2.$x
done
