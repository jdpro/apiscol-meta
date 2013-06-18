var test = {
	"_id" : "a25e7879-ecb3-4f0c-bd05-16867a1351cd",
	"@xmlns" : "http://ltsc.ieee.org/xsd/LOM",
	"@xmlns:lomfr" : "http://www.lom-fr.fr/xsd/LOMFR",
	"@xmlns:scolomfr" : "http://www.lom-fr.fr/xsd/SCOLOMFR",
	"@xmlns:xsi" : "http://www.w3.org/2001/XMLSchema-instance",
	"@xsi:schemaLocation" : "http://www.lom-fr.fr/scolomfr/fileadmin/user_upload/xsd/scolomfrv1.1/scolomfr.xsd",
	"general" : {
		"identifier" : {
			"catalog" : null,
			"entry" : "http://localhost:8080/content/resource/1379f1b7-737b-467d-9435-a700e8fded1d?format=xml"
		},
		"title" : {
			"string" : {
				"@language" : "fre",
				"#text" : "Exercices sur la création monétaire"
			}
		},
		"language" : "fre",
		"description" : {
			"string" : {
				"@language" : "fre",
				"#text" : "Par un glisser déposer d'étiquettes, mettre en évidence les écritures comptables auxquelles donne lieu une opération de création monétaire."
			}
		},
		"keyword" : [ {
			"string" : {
				"@language" : "fre",
				"#text" : "Création monétaire"
			}
		}, {
			"string" : {
				"@language" : "fre",
				"#text" : "Comptes en T"
			}
		}, {
			"string" : {
				"@language" : "fre",
				"#text" : "Banque"
			}
		}, {
			"string" : {
				"@language" : "fre",
				"#text" : "Bilan"
			}
		} ],
		"aggregationLevel" : {
			"source" : "LOMv1.0",
			"value" : "1"
		}
	},
	"lifeCycle" : {
		"status" : {
			"source" : "LOMv1.0",
			"value" : "final"
		},
		"contribute" : [
				{
					"role" : {
						"source" : "LOMv1.0",
						"value" : "publisher"
					},
					"entity" : "BEGIN:VCARDVERSION:3.0ORG:SES VersaillesEND:VCARD",
					"date" : {
						"dateTime" : "2012-10-16"
					}
				},
				{
					"role" : {
						"source" : "LOMv1.0",
						"value" : "author"
					},
					"entity" : "BEGIN:VCARDVERSION:3.0FN:Dornbusch, JoachimN:Dornbusch, JoachimEND:VCARD",
					"date" : {
						"dateTime" : "2010-10-10"
					}
				} ]
	},
	"metaMetadata" : {
		"identifier" : {
			"catalog" : "Apiscol :example",
			"entry" : "http://localhost:8080/meta/a25e7879-ecb3-4f0c-bd05-16867a1351cd"
		},
		"contribute" : {
			"role" : {
				"source" : "LOMv1.0",
				"value" : "creator"
			},
			"entity" : "BEGIN:VCARDVERSION:3.0FN:Dornbusch, JoachimORG:CRDP de l'académie de la VersaillesEND:VCARD",
			"date" : {
				"dateTime" : "2012-10-16"
			}
		},
		"metadataSchema" : "SCOLOMFRv1.0",
		"language" : "fre"
	},
	"technical" : {
		"format" : "application/x-shockwave-flash",
		"size" : "485",
		"location" : "http://localhost:8080/content/resources/1/3/7/9f1b7-737b-467d-9435-a700e8fded1d/Glup_1huitieme.swf",
		"requirement" : {
			"orComposite" : {
				"type" : {
					"source" : "LOMv1.0",
					"value" : "browser"
				},
				"name" : {
					"source" : "LOMv1.0",
					"value" : "any"
				}
			}
		}
	},
	"educational" : {
		"intendedEndUserRole" : {
			"source" : "LOMv1.0",
			"value" : "learner"
		},
		"learningResourceType" : {
			"source" : "LOMFRv1.0",
			"value" : "animation"
		},
		"typicalLearningTime" : {
			"duration" : "P0Y0M0DT2H0M"
		},
		"context" : [ {
			"source" : "LOMv1.0",
			"value" : "school"
		}, {
			"source" : "LOMFRv1.0",
			"value" : "enseignement secondaire"
		} ],
		"description" : [
				{
					"string" : {
						"@language" : "fre",
						"#text" : "Utilisable collectivement sur le tableau numérique intéractif."
					}
				}, null ],
		"language" : "fre",
		"lomfr:activity" : {
			"@xmlns:lomfr" : "http://www.lom-fr.fr/xsd/LOMFR",
			"lomfr:source" : "LOMFRv1.0",
			"lomfr:value" : "apprendre"
		},
		"scolomfr:place" : {
			"@xmlns:scolomfr" : "http://www.lom-fr.fr/xsd/SCOLOMFR",
			"scolomfr:source" : "SCOLOMFRv1.0",
			"scolomfr:value" : "en salle de classe"
		},
		"scolomfr:educationalMethod" : {
			"@xmlns:scolomfr" : "http://www.lom-fr.fr/xsd/SCOLOMFR",
			"scolomfr:source" : "SCOLOMFRv1.1",
			"scolomfr:value" : "en classe entière"
		},
		"scolomfr:tool" : [ {
			"@xmlns:scolomfr" : "http://www.lom-fr.fr/xsd/SCOLOMFR",
			"scolomfr:source" : "SCOLOMFRv1.0",
			"scolomfr:value" : "TBI"
		}, {
			"@xmlns:scolomfr" : "http://www.lom-fr.fr/xsd/SCOLOMFR",
			"scolomfr:source" : "SCOLOMFRv1.0",
			"scolomfr:value" : "tablette informatique"
		} ]
	},
	"rights" : {
		"cost" : {
			"source" : "LOMv1.0",
			"value" : "no"
		},
		"copyrightAndOtherRestrictions" : {
			"source" : "LOMv1.0",
			"value" : "yes"
		},
		"description" : {
			"string" : {
				"@language" : "fre",
				"#text" : "CC Paternité, pas d'utilisation commerciale"
			}
		}
	},
	"relation" : [
			null,
			{
				"kind" : {
					"source" : "SCOLOMFRv1.0",
					"value" : "a pour aperçu"
				},
				"resource" : {
					"identifier" : "URI",
					"entry" : "http://localhost:8080/content/previews/1/3/7/9f1b7-737b-467d-9435-a700e8fded1d/index.html"
				}
			},
			{
				"kind" : {
					"source" : "SCOLOMFRv1.0",
					"value" : "a pour vignette"
				},
				"resource" : {
					"identifier" : "URI",
					"entry" : "http://localhost:8080/thumbs/files/f/5/3/8d8ef1bdb962082caab8dc841c87c.png"
				}
			} ],
	"classification" : [ {
		"purpose" : {
			"source" : "LOMv1.0",
			"value" : "discipline"
		},
		"taxonPath" : [ {
			"source" : {
				"string" : {
					"@language" : "fre",
					"#text" : "Nomenclature disciplines générales"
				}
			},
			"taxon" : {
				"id" : "SES",
				"entry" : {
					"string" : {
						"@language" : "fre",
						"#text" : "Sciences Économiques et Sociales"
					}
				}
			}
		}, {
			"source" : {
				"string" : {
					"@language" : "fre",
					"#text" : "Programme SES 1ère 2012"
				}
			},
			"taxon" : {
				"id" : "T4",
				"entry" : {
					"string" : {
						"@language" : "fre",
						"#text" : "Qui crée la monnaie ?"
					}
				}
			}
		} ]
	}, {
		"purpose" : {
			"source" : "LOMv1.0",
			"value" : "educational level"
		},
		"taxonPath" : [ {
			"source" : {
				"string" : {
					"@language" : "fre",
					"#text" : "scolomfr-voc-022"
				}
			},
			"taxon" : [ {
				"id" : "scolomfr-voc-022-num-027",
				"entry" : {
					"string" : {
						"@language" : "fre",
						"#text" : "1ère"
					}
				}
			}, {
				"id" : "scolomfr-voc-022-num-087",
				"entry" : {
					"string" : {
						"@language" : "fre",
						"#text" : "lycée général"
					}
				}
			} ]
		}, {
			"source" : {
				"string" : {
					"@language" : "fre",
					"#text" : "Modules Elémentaires de formation"
				}
			},
			"taxon" : {
				"id" : "2472210631",
				"entry" : {
					"string" : {
						"@language" : "fre",
						"#text" : "Première ES"
					}
				}
			}
		} ]
	}, {
		"purpose" : {
			"source" : "SCOLOMFRv1.0",
			"value" : "public cible détaillé"
		},
		"taxonPath" : {
			"source" : {
				"string" : {
					"@language" : "fre",
					"#text" : "scolomfr-voc-021"
				}
			},
			"taxon" : {
				"id" : "scolomfr-voc-021-num-00092",
				"entry" : {
					"string" : {
						"@language" : "fre",
						"#text" : "professeur de lycée"
					}
				}
			}
		}
	} ]
}
