# kafka trace propagation example request
# message publishing order : reservation.created -> payment.approved -> ticket.issued
# transaction flow : reservation -> payment -> ticket -> reservation
curl --location --request POST 'http://localhost:8083/v1/reservations'