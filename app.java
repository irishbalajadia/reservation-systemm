// 1. DOMAIN CLASSES
class Facility {
    constructor(id, name, type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.bookedSlots = [];
    }

    isTimelineOverlapping(date, startMins, endMins) {
        return this.bookedSlots.some(slot => {
            if (slot.date !== date) return false;
            return (startMins < slot.endMins && endMins > slot.startMins);
        });
    }

    reserveSlot(date, label, startMins, endMins) {
        this.bookedSlots.push({ date, label, startMins, endMins });
    }

    releaseSlot(date, startMins, endMins) {
        this.bookedSlots = this.bookedSlots.filter(slot =>
            !(slot.date === date && slot.startMins === startMins && slot.endMins === endMins)
        );
    }
}

class Reservation {
    constructor(id, facilityInstance, userName, userRole, date, durationLabel, startMins, endMins) {
        this.id = id;
        this.facility = facilityInstance;
        this.userName = userName;
        this.userRole = userRole;
        this.date = date;
        this.durationLabel = durationLabel;
        this.startMins = startMins;
        this.endMins = endMins;
    }
}

// 2. SYSTEM MANAGER CLASS
class ReservationSystem {
    constructor() {
        this.facilities = [];
        this.reservations = [];
        this.searchQuery = "";
    }

    addFacility(facility) {
        this.facilities.push(facility);
    }

    createReservation(facilityId, userName, userRole, date, durationType, customStart, customEnd) {
        const facility = this.facilities.find(f => f.id === facilityId);
        if (!facility) return { success: false, message: "Facility not found." };

        const selectedDate = new Date(date + 'T00:00:00');
        const todayDate = new Date();
        todayDate.setHours(0, 0, 0, 0);
        if (selectedDate < todayDate) {
            return { success: false, message: "Cannot reserve slots for past dates." };
        }

        let startMins = 0;
        let endMins = 0;
        let durationLabel = "";

        if (durationType === "Whole Day") {
            startMins = 480;
            endMins = 1020;
            durationLabel = "Whole Day (08:00 AM - 05:00 PM)";
        } else if (durationType === "Half Day (AM)") {
            startMins = 480;
            endMins = 720;
            durationLabel = "Half Day AM (08:00 AM - 12:00 PM)";
        } else if (durationType === "Half Day (PM)") {
            startMins = 780;
            endMins = 1020;
            durationLabel = "Half Day PM (01:00 PM - 05:00 PM)";
        } else if (durationType === "Custom Time Slot") {
            if (!customStart || !customEnd) {
                return { success: false, message: "Please specify custom start and end times." };
            }

            const parseToMins = (timeStr) => {
                const [h, m] = timeStr.split(':').map(Number);
                return (h * 60) + m;
            };

            startMins = parseToMins(customStart);
            endMins = parseToMins(customEnd);

            if (startMins >= endMins) {
                return { success: false, message: "End time must be later than the start time!" };
            }

            durationLabel = Custom (${customStart} to ${customEnd});
        }

        if (facility.isTimelineOverlapping(date, startMins, endMins)) {
            return { success: false, message: Conflict detected! ${facility.name} is already booked during this timeframe on ${date}. };
        }

        const reservationId = 'RES-' + Date.now();
        const newReservation = new Reservation(reservationId, facility, userName, userRole, date, durationLabel, startMins, endMins);

        this.reservations.push(newReservation);
        facility.reserveSlot(date, durationLabel, startMins, endMins);

        this.updateUI();
        return { success: true, message: "Space reserved successfully!" };
    }

    cancelReservation(reservationId) {
        const index = this.reservations.findIndex(r => r.id === reservationId);
        if (index === -1) return;
    
