package com.yowyob.rideandgo.application.service;

import org.springframework.stereotype.Service;

@Service
public class TrackingCalculatorService {

    private static final int EARTH_RADIUS_KM = 6371;
    // Vitesse moyenne en ville estimée à 30 km/h pour le MVP
    private static final double AVERAGE_SPEED_KMH = 30.0; 

    /**
     * Calcule la distance à vol d'oiseau (Formule de Haversine).
     * @return Distance en kilomètres.
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0.0;
        }

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        double distance = EARTH_RADIUS_KM * c;
        
        // Arrondi à 2 décimales (ex: 1.54 km)
        return Math.round(distance * 100.0) / 100.0;
    }

    /**
     * Estime le temps d'arrivée (ETA) basé sur la distance et une vitesse moyenne.
     * @param distanceKm La distance en kilomètres.
     * @return Temps estimé en minutes (arrondi à l'entier supérieur).
     */
    public int calculateEtaInMinutes(double distanceKm) {
        if (distanceKm <= 0) return 0;

        // Temps (heures) = Distance / Vitesse
        double timeInHours = distanceKm / AVERAGE_SPEED_KMH;
        
        // Conversion en minutes
        double timeInMinutes = timeInHours * 60;

        // On arrondit toujours à la minute supérieure pour ne pas être en retard
        return (int) Math.ceil(timeInMinutes);
    }
}