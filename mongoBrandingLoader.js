db = db.getSiblingDB('test2')
db.matches.createIndex( { place_id: 1 } )
db.master.find().forEach(function(hotel){
    var  placeId = hotel._id
    var branding = db.matches.findOne({place_id: placeId})
    if (!branding) print ("Warning!!"+placeId+" "+hotel.name)
    delete branding.place_id
    delete branding.hotelAddress
    delete branding.hotelName
    delete branding.hotelWebsite
    hotel.branding = branding
    db.master.save(hotel)
})

