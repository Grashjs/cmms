import React, { useEffect, useRef, useState } from 'react';
import {
  GoogleMap,
  InfoWindow,
  Marker,
  withGoogleMap,
  withScriptjs
} from 'react-google-maps';
import { mapStyle } from './mapStyle';
import { googleMapsConfig } from '../../../../config';
import { Box, Link, Typography } from '@mui/material';

interface Location {
  id: number;
  title: string;
  address: string;
  coordinates: { lat: number; lng: number };
}
interface MapProps {
  dimensions?: { width: number; height: number };
  locations?: Location[];
  select?: boolean;
  selected?: { lat: number; lng: number };
  onSelect?: (coordinates: { lat: number; lng: number }) => void;
}

function LocalMap({ locations, select, onSelect, selected }) {
  const mapRef = useRef<GoogleMap>(null);
  const [selectedLocation, setSelectedLocation] = useState<Location>();
  const [selectedCoordinates, setSelectedCoordinates] = useState<{
    lat: number;
    lng: number;
  }>();

  useEffect(() => {
    const bounds = new window.google.maps.LatLngBounds();
    if (locations.length) {
      locations.forEach((location) => bounds.extend(location.coordinates));
      mapRef.current.fitBounds(bounds);
    }
  }, [mapRef]);

  const defaultCenter = { lat: 31.1728205, lng: -7.3362482 };
  return (
    <GoogleMap
      ref={mapRef}
      onClick={(event) => {
        if (select && onSelect) {
          const coordinates = {
            lat: event.latLng.lat(),
            lng: event.latLng.lng()
          };
          setSelectedCoordinates(coordinates);
          onSelect(coordinates);
        }
      }}
      defaultCenter={selected ?? defaultCenter}
      defaultZoom={6}
      defaultOptions={{ styles: mapStyle }}
      options={{ streetViewControl: false }}
    >
      {!select && (
        <>
          {locations.map((location, index) => (
            <Marker
              key={index}
              position={location.coordinates}
              title={location.title}
              onClick={() => setSelectedLocation(location)}
              icon={{
                url: '/static/images/markers/red.png',
                scaledSize: new window.google.maps.Size(25, 25)
              }}
            />
          ))}
          {selectedLocation && (
            <InfoWindow
              onCloseClick={() => setSelectedLocation(null)}
              position={selectedLocation.coordinates}
            >
              <Box>
                <Link
                  variant="h6"
                  color="primary"
                  href={`/app/locations/${selectedLocation.id}`}
                >
                  {selectedLocation.title}
                </Link>
                <Typography variant="subtitle1">
                  {selectedLocation.address}
                </Typography>
              </Box>
            </InfoWindow>
          )}
        </>
      )}
      {select && (
        <Marker
          position={selectedCoordinates ?? selected}
          icon={{
            url: '/static/images/markers/red.png',
            scaledSize: new window.google.maps.Size(25, 25)
          }}
        />
      )}
    </GoogleMap>
  );
}
export default function Map({
  dimensions,
  locations = [],
  select,
  selected,
  onSelect
}: MapProps) {
  const { apiKey } = googleMapsConfig;

  const MapWrapped = withScriptjs(
    withGoogleMap(() => (
      <LocalMap
        locations={locations}
        select={select}
        onSelect={onSelect}
        selected={selected}
      />
    ))
  );

  return (
    <div
      style={{
        width: dimensions.width ?? 500,
        height: dimensions.height ?? 500
      }}
    >
      <MapWrapped
        googleMapURL={`https://maps.googleapis.com/maps/api/js?v=3.exp&libraries=geometry,drawing,places&key=${apiKey}`}
        loadingElement={<div style={{ height: `100%` }} />}
        containerElement={<div style={{ height: `100%` }} />}
        mapElement={<div style={{ height: `100%` }} />}
      />
    </div>
  );
}
