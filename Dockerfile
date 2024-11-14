FROM node:23-alpine3.19

COPY . . 

RUN rm -rf /.git

RUN npm install
RUN npm run build
CMD npm run start