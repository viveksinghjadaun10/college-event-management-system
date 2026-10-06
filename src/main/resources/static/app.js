const API = "/api";

const MONTHS = ["JAN", "FEB", "MAR", "APR", "MAY", "JUN",
    "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"];

const eventsById = {};
let selectedEventId = null;

function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text ?? "";
    return div.innerHTML;
}

function formatDate(isoDate) {
    if (!isoDate) return "";
    const [year, month, day] = isoDate.split("-");
    return `${day.padStart(2, "0")} ${MONTHS[parseInt(month, 10) - 1]} ${year}`;
}

function todayIso() {
    const d = new Date();
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${d.getFullYear()}-${month}-${day}`;
}

async function readJson(response) {
    try {
        return await response.json();
    } catch (error) {
        return null;
    }
}

function errorText(body, fallback) {
    if (body && body.message) return body.message;
    if (body && body.errors) return Object.values(body.errors).join(". ");
    return fallback;
}

function showMessage(text, isError) {
    const el = document.getElementById("register-message");
    el.textContent = text;
    el.className = isError ? "msg-error" : "msg-success";
}

async function loadEvents() {
    const container = document.getElementById("events-list");

    try {
        const response = await fetch(`${API}/events`);
        const events = await response.json();

        if (events.length === 0) {
            container.innerHTML = "<p>No events yet.</p>";
            return;
        }

        let html = `
      <table>
        <tr>
          <th>ID</th><th>Name</th><th>Date</th><th>Venue</th>
          <th>Category</th><th>Seats left</th><th>Register</th>
        </tr>`;

        for (const event of events) {
            eventsById[event.id] = event;

            const regResponse = await fetch(`${API}/registrations/event/${event.id}`);
            const registrations = await regResponse.json();
            const seatsLeft = event.totalSeats - registrations.length;
            const isPast = event.eventDate < todayIso();

            let seatsText;
            let action;
            if (seatsLeft <= 0) {
                seatsText = `<span class="full">Full</span>`;
                action = `<span class="full">Full</span>`;
            } else {
                seatsText = `${seatsLeft} of ${event.totalSeats}`;
                action = isPast
                    ? `<span class="closed">Closed</span>`
                    : `<button class="register-btn" data-id="${event.id}">Register</button>`;
            }

            html += `
        <tr>
          <td>${event.id}</td>
          <td>${escapeHtml(event.name)}</td>
          <td>${escapeHtml(formatDate(event.eventDate))}</td>
          <td>${escapeHtml(event.venue)}</td>
          <td>${escapeHtml(event.category)}</td>
          <td>${seatsText}</td>
          <td>${action}</td>
        </tr>`;
        }

        html += "</table>";
        container.innerHTML = html;

        container.querySelectorAll(".register-btn").forEach(button => {
            button.addEventListener("click", () => openRegister(Number(button.dataset.id)));
        });
    } catch (error) {
        container.innerHTML = "<p>Could not load events. Is the app running?</p>";
        console.error(error);
    }
}

function openRegister(eventId) {
    const event = eventsById[eventId];
    selectedEventId = eventId;

    document.getElementById("register-title").textContent =
        `Register for ${event.name} (${formatDate(event.eventDate)})`;

    for (const id of ["reg-name", "reg-email", "reg-department", "reg-mobile", "reg-otp"]) {
        document.getElementById(id).value = "";
    }

    document.getElementById("step-details").hidden = false;
    document.getElementById("step-otp").hidden = true;
    document.getElementById("ticket-link").hidden = true;
    showMessage("", false);

    const section = document.getElementById("register-section");
    section.hidden = false;
    section.scrollIntoView({ behavior: "smooth" });
}

async function sendOtp() {
    const name = document.getElementById("reg-name").value.trim();
    const email = document.getElementById("reg-email").value.trim();
    const mobile = document.getElementById("reg-mobile").value.trim();

    if (!name || !email) {
        showMessage("Enter your name and email.", true);
        return;
    }
    if (!/^[6-9]\d{9}$/.test(mobile)) {
        showMessage("Enter a valid 10-digit mobile number.", true);
        return;
    }

    const button = document.getElementById("send-otp-btn");
    button.disabled = true;
    showMessage("Sending OTP...", false);

    try {
        const response = await fetch(`${API}/register/send-otp`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ eventId: selectedEventId, mobileNumber: mobile })
        });
        const body = await readJson(response);

        if (!response.ok) {
            showMessage(errorText(body, "Could not send OTP."), true);
            return;
        }

        document.getElementById("otp-info").textContent = body.message;
        document.getElementById("step-details").hidden = true;
        document.getElementById("step-otp").hidden = false;
        document.getElementById("reg-otp").value = "";
        showMessage("", false);
    } catch (error) {
        showMessage("Could not reach the server.", true);
    } finally {
        button.disabled = false;
    }
}

async function verifyOtp() {
    const otp = document.getElementById("reg-otp").value.trim();
    if (!/^\d{6}$/.test(otp)) {
        showMessage("Enter the 6-digit OTP.", true);
        return;
    }

    const button = document.getElementById("verify-btn");
    button.disabled = true;
    showMessage("Verifying...", false);

    try {
        const response = await fetch(`${API}/register/verify`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                eventId: selectedEventId,
                mobileNumber: document.getElementById("reg-mobile").value.trim(),
                otp: otp,
                name: document.getElementById("reg-name").value.trim(),
                email: document.getElementById("reg-email").value.trim(),
                department: document.getElementById("reg-department").value.trim()
            })
        });
        const body = await readJson(response);

        if (!response.ok) {
            showMessage(errorText(body, "Could not verify the OTP."), true);
            return;
        }

        document.getElementById("step-otp").hidden = true;
        document.getElementById("step-details").hidden = true;
        showMessage(
            `Registered for ${body.eventName} on ${formatDate(body.eventDate)} at ${body.venue}. ` +
            `Your registration ID is ${body.registrationId}.`,
            false
        );

        const ticketLink = document.getElementById("ticket-link");
        ticketLink.href = `${API}/tickets/${encodeURIComponent(body.ticketCode)}`;
        ticketLink.hidden = false;

        loadEvents();
    } catch (error) {
        showMessage("Could not reach the server.", true);
    } finally {
        button.disabled = false;
    }
}

function backToDetails() {
    document.getElementById("step-otp").hidden = true;
    document.getElementById("step-details").hidden = false;
    showMessage("", false);
}

document.getElementById("send-otp-btn").addEventListener("click", sendOtp);
document.getElementById("verify-btn").addEventListener("click", verifyOtp);
document.getElementById("back-btn").addEventListener("click", backToDetails);

loadEvents();