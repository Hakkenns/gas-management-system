// Mapbox GL JS map initialization and GPS tracking for delivery detail view

let map;
let customerMarker;
let driverMarker;
let polylineSource;
let watchId;
let customerCoords = null;
let driverCoords = null;
let isFirstLoad = true;
const MAPBOX_TOKEN = 'pk.eyJ1IjoiZXNuYXlkZWhlciIsImEiOiJjbXJvNDluajkwaTVoMzZvaHh6MWI1Mzd1In0.OzlKmShVDi44fm9Xi8yJZw';

// Initialize the map when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    initializeMap();
});

async function initializeMap() {
    // Get customer coordinates from the rendered HTML element
    const mapElement = document.getElementById('map');
    const latitud = mapElement ? mapElement.getAttribute('data-latitud') : null;
    const longitud = mapElement ? mapElement.getAttribute('data-longitud') : null;
    
    console.log('COORDENADAS DEL PEDIDO:', latitud, longitud);
    
    // Use coordinates from database if available
    if (latitud && longitud && latitud !== '' && longitud !== '') {
        customerCoords = { 
            lat: parseFloat(latitud), 
            lng: parseFloat(longitud) 
        };
        console.log('Usando coordenadas de la base de datos:', customerCoords);
        initializeMapWithCoords();
        return;
    }
    
    // Fallback: Default to Chiclayo center
    console.warn('No hay coordenadas en la base de datos, usando Chiclayo centro por defecto');
    customerCoords = { lat: -6.7714, lng: -79.8465 };
    initializeMapWithCoords();
}

function initializeMapWithCoords() {
    // Set Mapbox access token
    mapboxgl.accessToken = MAPBOX_TOKEN;

    // Initialize Mapbox map centered on customer location
    map = new mapboxgl.Map({
        container: 'map',
        style: 'mapbox://styles/mapbox/streets-v12',
        center: [customerCoords.lng, customerCoords.lat],
        zoom: 15
    });

    // Add navigation controls
    map.addControl(new mapboxgl.NavigationControl());

    // Wait for map to load before adding markers
    map.on('load', function() {
        // Add customer marker
        addCustomerMarker(customerCoords.lat, customerCoords.lng);

        // Show visual feedback if using default coordinates
        const distanceElement = document.getElementById('distancia-reparto');
        if (distanceElement && customerCoords.lat === -6.7714 && customerCoords.lng === -79.8465) {
            distanceElement.innerHTML = '<i class="fas fa-exclamation-triangle"></i> Usando ubicación por defecto (Chiclayo centro)';
            distanceElement.classList.remove('text-primary');
            distanceElement.classList.add('text-warning');
        }

        // Start GPS tracking for driver
        startDriverTracking();
    });
}

function addCustomerMarker(lat, lng) {
    // Create custom marker element for customer
    const markerElement = document.createElement('div');
    markerElement.style.width = '30px';
    markerElement.style.height = '30px';
    markerElement.style.backgroundColor = '#dc3545';
    markerElement.style.borderRadius = '50%';
    markerElement.style.border = '3px solid white';
    markerElement.style.boxShadow = '0 2px 5px rgba(0,0,0,0.3)';
    markerElement.style.display = 'flex';
    markerElement.style.alignItems = 'center';
    markerElement.style.justifyContent = 'center';
    markerElement.innerHTML = '<i class="fas fa-home" style="color: white; font-size: 14px;"></i>';

    customerMarker = new mapboxgl.Marker({
        element: markerElement
    })
    .setLngLat([lng, lat])
    .addTo(map)
    .setPopup(new mapboxgl.Popup({ offset: 25 }).setHTML('<strong>Destino</strong><br>Ubicación del cliente'));
}

function startDriverTracking() {
    if (!navigator.geolocation) {
        console.error('Geolocation is not supported by your browser');
        updateDistanceDisplay('GPS no disponible');
        return;
    }

    // Start watching driver's position
    watchId = navigator.geolocation.watchPosition(
        updateDriverPosition,
        handleGeolocationError,
        {
            enableHighAccuracy: true,
            timeout: 10000,
            maximumAge: 0
        }
    );
}

function updateDriverPosition(position) {
    const lat = position.coords.latitude;
    const lng = position.coords.longitude;
    
    driverCoords = { lat, lng };

    // Add or update driver marker
    if (!driverMarker) {
        addDriverMarker(lat, lng);
    } else {
        driverMarker.setLngLat([lng, lat]);
    }

    // Update polyline between driver and customer
    updatePolyline();

    // Calculate and display distance
    const distance = calculateDistance(driverCoords, customerCoords);
    updateDistanceDisplay(distance);

    // Fit map to show both markers only on first load
    if (isFirstLoad && customerMarker && driverMarker) {
        const bounds = new mapboxgl.LngLatBounds([
            customerMarker.getLngLat(),
            driverMarker.getLngLat()
        ]);
        map.fitBounds(bounds, { padding: 50 });
        isFirstLoad = false;
    }
}

function addDriverMarker(lat, lng) {
    // Create custom marker element for driver (motorizado)
    const markerElement = document.createElement('div');
    markerElement.style.width = '35px';
    markerElement.style.height = '35px';
    markerElement.style.backgroundColor = '#28a745';
    markerElement.style.borderRadius = '50%';
    markerElement.style.border = '3px solid white';
    markerElement.style.boxShadow = '0 2px 5px rgba(0,0,0,0.3)';
    markerElement.style.display = 'flex';
    markerElement.style.alignItems = 'center';
    markerElement.style.justifyContent = 'center';
    markerElement.innerHTML = '<i class="fas fa-motorcycle" style="color: white; font-size: 16px;"></i>';

    driverMarker = new mapboxgl.Marker({
        element: markerElement
    })
    .setLngLat([lng, lat])
    .addTo(map)
    .setPopup(new mapboxgl.Popup({ offset: 25 }).setHTML('<strong>Tu ubicación</strong><br>Motorizado en movimiento'));
}

function updatePolyline() {
    if (!driverCoords || !customerCoords) return;

    // Remove existing polyline source if exists
    if (polylineSource) {
        map.removeLayer('polyline');
        map.removeSource('polyline');
    }

    // Add polyline source
    map.addSource('polyline', {
        'type': 'geojson',
        'data': {
            'type': 'Feature',
            'properties': {},
            'geometry': {
                'type': 'LineString',
                'coordinates': [
                    [driverCoords.lng, driverCoords.lat],
                    [customerCoords.lng, customerCoords.lat]
                ]
            }
        }
    });

    // Add polyline layer
    map.addLayer({
        'id': 'polyline',
        'type': 'line',
        'source': 'polyline',
        'layout': {
            'line-join': 'round',
            'line-cap': 'round'
        },
        'paint': {
            'line-color': '#05a660',
            'line-width': 4,
            'line-opacity': 0.7,
            'line-dasharray': [2, 2]
        }
    });

    polylineSource = 'polyline';
}

// Haversine Formula to calculate distance between two coordinates in meters
function calculateDistance(coords1, coords2) {
    const R = 6371000; // Earth's radius in meters
    const φ1 = coords1.lat * Math.PI / 180;
    const φ2 = coords2.lat * Math.PI / 180;
    const Δφ = (coords2.lat - coords1.lat) * Math.PI / 180;
    const Δλ = (coords2.lng - coords1.lng) * Math.PI / 180;

    const a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
              Math.cos(φ1) * Math.cos(φ2) *
              Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    const distance = R * c; // Distance in meters
    return distance;
}

function updateDistanceDisplay(distanceInMeters) {
    const distanceElement = document.getElementById('distancia-reparto');
    if (!distanceElement) return;

    if (typeof distanceInMeters === 'string') {
        distanceElement.innerHTML = `<i class="fas fa-route"></i> ${distanceInMeters}`;
        return;
    }

    let displayText;
    if (distanceInMeters < 1000) {
        displayText = `${Math.round(distanceInMeters)} metros`;
    } else {
        const km = (distanceInMeters / 1000).toFixed(1);
        displayText = `${km} km`;
    }

    distanceElement.innerHTML = `<i class="fas fa-route"></i> Distancia: ${displayText}`;
}

function handleGeolocationError(error) {
    console.error('Geolocation error:', error);
    let errorMessage = 'Error de GPS';
    
    switch(error.code) {
        case error.PERMISSION_DENIED:
            errorMessage = 'Permiso de GPS denegado';
            break;
        case error.POSITION_UNAVAILABLE:
            errorMessage = 'Ubicación no disponible';
            break;
        case error.TIMEOUT:
            errorMessage = 'Tiempo de espera agotado';
            break;
    }
    
    updateDistanceDisplay(errorMessage);
}

// Stop GPS tracking when leaving the page
window.addEventListener('beforeunload', function() {
    if (watchId) {
        navigator.geolocation.clearWatch(watchId);
    }
});
