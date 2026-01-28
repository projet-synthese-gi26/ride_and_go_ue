import requests
import json

# CONFIGURATION
# Remplacez par l'URL réelle de votre service de notification
NOTIFICATION_SERVICE_URL = "https://notification-service.pynfi.com/api/v1/templates" 
SERVICE_TOKEN = "a77599d3-8de7-4d52-b9d0-2202b2e13a9e" # Token obtenu lors de l'enregistrement du service Ride&Go

# --- STYLES (CSS Inliné pour emails) ---
HEADER_STYLE = "background-color: #4CAF50; color: white; padding: 20px; text-align: center; font-family: Arial, sans-serif;"
BODY_STYLE = "padding: 20px; font-family: Arial, sans-serif; color: #333; line-height: 1.6;"
BUTTON_STYLE = "background-color: #4CAF50; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; display: inline-block; font-weight: bold;"
FOOTER_STYLE = "background-color: #f4f4f4; color: #888; padding: 15px; text-align: center; font-size: 12px; font-family: Arial, sans-serif;"
CARD_STYLE = "border: 1px solid #ddd; border-radius: 8px; padding: 15px; background-color: #f9f9f9; margin: 15px 0;"

def get_html_wrapper(title, content):
    return f"""
    <!DOCTYPE html>
    <html>
    <body style="margin: 0; padding: 0; background-color: #ffffff;">
        <table width="100%" border="0" cellspacing="0" cellpadding="0">
            <tr>
                <td align="center">
                    <table width="600" border="0" cellspacing="0" cellpadding="0" style="border: 1px solid #e0e0e0;">
                        <tr><td style="{HEADER_STYLE}"><h1 style="margin:0;">Ride & Go</h1><p style="margin:5px 0 0;">{title}</p></td></tr>
                        <tr><td style="{BODY_STYLE}">{content}</td></tr>
                        <tr><td style="{FOOTER_STYLE}">&copy; 2026 Ride & Go. Tous droits réservés.</td></tr>
                    </table>
                </td>
            </tr>
        </table>
    </body>
    </html>
    """

# --- LISTE DES TEMPLATES À CRÉER ---
# Note : Pas de 'templateId' ici, car c'est le serveur qui le donne (ou on le passe si l'API le permet, mais suivons le swagger strict)

templates_to_create = [
    {
        "key": "new-offer", # Clé interne pour nous repérer
        "payload": {
            "name": "New Offer (Driver)",
            "description": "Notification aux chauffeurs pour une nouvelle course disponible",
            "type": "EMAIL",
            "fromEmail": "noreply@rideandgo.com",
            "subject": "🚗 Nouvelle course disponible : {{start}} -> {{end}}",
            "message": "Nouvelle course disponible", # Fallback texte brut
            "bodyHtml": get_html_wrapper("Nouvelle Opportunité", """
                <p>Bonjour,</p>
                <p>Une nouvelle course correspondant à votre zone est disponible !</p>
                <div style="{CARD_STYLE}">
                    <p><strong>Départ :</strong> {{start}}</p>
                    <p><strong>Arrivée :</strong> {{end}}</p>
                    <p><strong>Prix :</strong> <span style="color: #4CAF50; font-weight: bold;">{{price}} FCFA</span></p>
                </div>
                <div style="text-align: center;"><a href="rideandgo://offers/{{offerId}}" style="{BUTTON_STYLE}">Voir l'offre</a></div>
            """.format(CARD_STYLE=CARD_STYLE, BUTTON_STYLE=BUTTON_STYLE))
        }
    },
    {
        "key": "driver-applied",
        "payload": {
            "name": "Driver Applied (Passenger)",
            "description": "Notification au client qu'un chauffeur a postulé",
            "type": "EMAIL",
            "fromEmail": "noreply@rideandgo.com",
            "subject": "👋 Un chauffeur est intéressé !",
            "message": "Un chauffeur a postulé à votre offre",
            "bodyHtml": get_html_wrapper("Candidature Reçue", """
                <p><strong>{{driverName}}</strong> est disponible pour effectuer votre course.</p>
                <div style="text-align: center;"><a href="rideandgo://offers/{{offerId}}/bids" style="{BUTTON_STYLE}">Voir le chauffeur</a></div>
            """.format(BUTTON_STYLE=BUTTON_STYLE))
        }
    },
    {
        "key": "driver-selected",
        "payload": {
            "name": "Driver Selected (Driver)",
            "description": "Notification au chauffeur qu'il a été choisi",
            "type": "EMAIL",
            "fromEmail": "noreply@rideandgo.com",
            "subject": "🎉 Vous avez été sélectionné !",
            "message": "Le client vous a choisi",
            "bodyHtml": get_html_wrapper("Félicitations !", """
                <p>Le client a accepté votre candidature pour <strong>{{price}} FCFA</strong>.</p>
                <div style="text-align: center;"><a href="rideandgo://offers/{{offerId}}/confirm" style="{BUTTON_STYLE}">Confirmer la course</a></div>
            """.format(BUTTON_STYLE=BUTTON_STYLE))
        }
    },
    {
        "key": "ride-confirmed",
        "payload": {
            "name": "Ride Confirmed (Passenger)",
            "description": "Le chauffeur arrive",
            "type": "EMAIL",
            "fromEmail": "noreply@rideandgo.com",
            "subject": "✅ Votre chauffeur est en route",
            "message": "Votre chauffeur arrive",
            "bodyHtml": get_html_wrapper("Chauffeur Confirmé", """
                <p>Votre chauffeur a confirmé la prise en charge et se dirige vers vous.</p>
                <div style="text-align: center;"><a href="rideandgo://rides/{{rideId}}/track" style="{BUTTON_STYLE}">Suivre mon chauffeur</a></div>
            """.format(BUTTON_STYLE=BUTTON_STYLE))
        }
    },
    {
        "key": "ride-cancelled",
        "payload": {
            "name": "Ride Cancelled",
            "description": "Annulation de course",
            "type": "EMAIL",
            "fromEmail": "noreply@rideandgo.com",
            "subject": "🚫 Course annulée",
            "message": "La course a été annulée",
            "bodyHtml": get_html_wrapper("Annulation", """
                <p>La course a été annulée.</p>
                <p>Si vous n'êtes pas à l'origine de cette annulation, contactez le support.</p>
            """)
        }
    }
]

def deploy_templates():
    headers = {
        "Content-Type": "application/json",
        "X-Service-Token": SERVICE_TOKEN
    }

    print(f"🚀 Déploiement vers {NOTIFICATION_SERVICE_URL}...")
    generated_ids = {}

    for item in templates_to_create:
        key = item['key']
        payload = item['payload']
        
        try:
            print(f"   -> Envoi {key}...")
            # Envoi strict selon le Swagger body
            response = requests.post(NOTIFICATION_SERVICE_URL, headers=headers, json=payload)
            
            if response.status_code in [200, 201]:
                data = response.json()
                # On suppose que la réponse contient 'templateId' comme dans le Swagger Response 201
                t_id = data.get('templateId') 
                print(f"   ✅ Créé avec ID: {t_id}")
                generated_ids[key] = t_id
            else:
                print(f"   ❌ Erreur {response.status_code}: {response.text}")
        except Exception as e:
            print(f"   ⚠️ Exception: {e}")

    print("\n--- 📋 IDS À COPIER DANS APPLICATION.YML ---")
    print("application:")
    print("  notification:")
    print("    templates:")
    for k, v in generated_ids.items():
        print(f"      {k}: {v}")

if __name__ == "__main__":
    deploy_templates()