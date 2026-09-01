const cards = [{
  name: "Bookshop",
  color: "673ab7"
}, {
  name: "Cafeteria",
  color: "795548"
}, {
  name: "Clothing Store",
  color: "ff4fa5"
}, {
  name: "Department Store",
  color: "000000"
}, {
  name: "Grocery Store",
  color: "4caf50"
}, {
  name: "Pharmacy",
  color: "00286e"
}, {
  name: "Restaurant",
  color: "59a2be"
}, {
  name: "Shoe Store",
  color: "9c27b0"
}]

// For some reason Maestro passes this as a string with the value "0.0"
var index = Math.round(INDEX)

console.log("Returning card " + (index + 1) + " of " + cards.length + " with name " + cards[index]['name'] + " and color " + cards[index]['color'])

output.cardCount = cards.length
output.cardName = cards[index]['name']
output.cardColor = cards[index]['color']
