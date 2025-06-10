package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig;
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() throws Exception{
        dataBaseTestConfig = new DataBaseTestConfig();

        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("123");

        // make clear for the database: put all parkingspot as available and clear the ticket table
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown(){

    }

    @Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        //check that a ticket is actualy saved in DB
        Ticket ticket =  ticketDAO.getTicket("123");
        Assertions.assertNotNull(ticket);
        assertEquals("123", ticket.getVehicleRegNumber());

        // Check Parking table is updated with availability
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        Assertions.assertFalse(parkingSpot.isAvailable());
    }

    @Test
    public void testParkingLotExit(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        testParkingACar();

        parkingService.processExitingVehicle();
        //check that the fare generated and out time are populated correctly in the database
        Ticket exitTicket =  ticketDAO.getTicket("123");
        Assertions.assertNotNull(exitTicket);
        assertEquals("123", exitTicket.getVehicleRegNumber());
        assertEquals(ParkingType.CAR, exitTicket.getParkingSpot().getParkingType());
        Assertions.assertNotNull(exitTicket.getOutTime());
        Assertions.assertNotNull(exitTicket.getPrice());

    }

    @Test
    public void testParkingLotExitRecurringUser(){
        //Insert a ticket to simulate a recurrent user
        testParkingLotExit();

        //Insert incoming ticket
        ParkingSpot parkingSpot = new ParkingSpot(2, ParkingType.CAR, false);
        parkingSpotDAO.updateParking(parkingSpot);

        Ticket secondTicket = new Ticket();
        secondTicket.setParkingSpot(parkingSpot);
        secondTicket.setVehicleRegNumber("123");
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  45 * 60 * 1000) );
        secondTicket.setInTime(inTime);
        secondTicket.setOutTime(null);
        ticketDAO.saveTicket(secondTicket);


        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();

        //check that a ticket is actualy update correctly in the DB
        Ticket exitTicket =  ticketDAO.getTicket("123");
        Assertions.assertNotNull(exitTicket);
        assertEquals("123", exitTicket.getVehicleRegNumber());

        // Check Parkingspot available
        Assertions.assertTrue(exitTicket.getParkingSpot().isAvailable());

        assertEquals(ParkingType.CAR, exitTicket.getParkingSpot().getParkingType());
        Assertions.assertNotNull(exitTicket.getOutTime());

        NumberFormat numberFormatter = NumberFormat.getNumberInstance();
        numberFormatter.setMaximumFractionDigits(3); // no decimal part
        String exitPrice = numberFormatter.format(exitTicket.getPrice());
        String expectedPrice = numberFormatter.format((0.75 * Fare.CAR_RATE_PER_HOUR));


        assertEquals(expectedPrice , exitPrice);

    }
}
