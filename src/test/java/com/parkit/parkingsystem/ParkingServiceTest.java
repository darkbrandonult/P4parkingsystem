package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach
    public void setUpPerTest() {
        try {
            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void processExitingVehicleTest() throws Exception {

        Ticket ticket = setupTicket();
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(ticket.getVehicleRegNumber());
        when(ticketDAO.getTicket(ticket.getVehicleRegNumber())).thenReturn(ticket);
        when(ticketDAO.getNbTicket(ticket.getVehicleRegNumber())).thenReturn(2);
        when(ticketDAO.updateTicket(ticket)).thenReturn(true);
        when(parkingSpotDAO.updateParking(ticket.getParkingSpot())).thenReturn(true);

        parkingService.processExitingVehicle();

        verify(ticketDAO, Mockito.times(1)).getNbTicket(any());
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
    }

    @Test
    public void testProcessIncomingVehicle() throws Exception {
        //test de l’appel de la méthode processIncomingVehicle() où tout se déroule comme attendu.
        String vehicleRegNumber ="123";
        ParkingType parkingType = ParkingType.CAR;
        int ParkingSpotNumber = 3;

        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegNumber);
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(parkingType)).thenReturn(ParkingSpotNumber);
        when(parkingSpotDAO.updateParking(any())).thenReturn(true);
        when(ticketDAO.saveTicket(any())).thenReturn(true);
        when(ticketDAO.getNbTicket(vehicleRegNumber)).thenReturn(2);

        parkingService.processIncomingVehicle();

        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).saveTicket(any());
        verify(ticketDAO, Mockito.times(1)).getNbTicket(vehicleRegNumber);

    }

    @Test
    public void processExitingVehicleTestUnableUpdate() throws Exception {
        //exécution du test dans le cas où la méthode updateTicket() deticketDAO renvoie false lors de l’appel de processExitingVehicle()

        Ticket ticket = setupTicket();
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(ticket.getVehicleRegNumber());
        when(ticketDAO.getTicket(ticket.getVehicleRegNumber())).thenReturn(ticket);
        when(ticketDAO.getNbTicket(ticket.getVehicleRegNumber())).thenReturn(2);
        when(ticketDAO.updateTicket(ticket)).thenReturn(false);

        parkingService.processExitingVehicle();

        verify(ticketDAO, Mockito.times(1)).getNbTicket(any());
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));

    }

    @Test
    public void testGetNextParkingNumberIfAvailable() throws Exception {
        //test de l’appel de la méthode getNextParkingNumberIfAvailable() avec pour résultat l’obtention d’un spot dont l’ID est 1
        // et qui est disponible.

        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);

        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        assertEquals(1, parkingSpot.getId());
        assertEquals(ParkingType.CAR, parkingSpot.getParkingType());
        assertEquals(true, parkingSpot.isAvailable());
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
        //test de l’appel de la méthode getNextParkingNumberIfAvailable() avec pour résultat
        // aucun spot disponible (la méthode renvoie null).

        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any())).thenReturn(-1);

        Exception exception = assertThrows(Exception.class, () -> parkingService.getNextParkingNumberIfAvailable());

        assertEquals("Error fetching parking number from DB. Parking slots might be full", exception.getMessage());
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
        //test de l’appel de la méthode getNextParkingNumberIfAvailable() avec pour résultat
        // aucun spot (la méthode renvoie null) car l’argument saisi par l’utilisateur concernant le type de véhicule est erroné (par exemple, l’utilisateur a saisi 3).

        when(inputReaderUtil.readSelection()).thenReturn(0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> parkingService.getNextParkingNumberIfAvailable());
        assertEquals("Entered input is invalid", exception.getMessage());
    }

    private Ticket setupTicket(){
        Ticket ticket = new Ticket();
        ticket.setId(1);
        ticket.setVehicleRegNumber("123");
        ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
        ticket.setOutTime(null);
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        ticket.setParkingSpot(parkingSpot);

        return ticket;
    }

}
