FROM node:23-alpine3.19

COPY . . 

WORKDIR /program
RUN rm -rf /.git

RUN apk add git

RUN git clone https://github.com/ModGardenEvent/gardenbot.git .

RUN npm install
RUN npm run build
CMD git pull && npm run start