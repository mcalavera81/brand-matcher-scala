var array=[];
db.matches.aggregate([ { $sample: { size: 1000 } } ]).forEach(function(match){
    array.push(match)
})
print(tojson(array))