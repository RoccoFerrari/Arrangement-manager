# create_db.py

import os
from server import app, db

# Usa un percorso assoluto per il database per evitare problemi di directory
basedir = os.path.abspath(os.path.dirname(__file__))
database_path = os.path.join(basedir, 'database.db')

if os.path.exists(database_path):
    print("Il file del database esiste già. Cancellalo e riprova.")
else:
    with app.app_context():
        print("Creazione del database e delle tabelle...")
        db.create_all()
        
        if os.path.exists(database_path):
            print(f"Database e tabelle creati con successo! File trovato in: {database_path}")
        else:
            print("Errore: Il database non è stato creato. Controlla la configurazione di SQLAlchemy.")