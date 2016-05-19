FROM mongo

COPY brands.json /brands.json
CMD mongoimport --drop --host mongodb --db test2 --collection brands --type json --file /brands.json \
    && mongoimport --drop --host mongodb --db test2 --collection master --type json --file /hotel_data/hotels_big.json --jsonArray