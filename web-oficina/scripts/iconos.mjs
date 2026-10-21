// Genera los PNG del manifest a partir de public/icono.svg. Se corre una vez: npm run iconos
import sharp from 'sharp'

for (const tam of [192, 512]) {
  await sharp('public/icono.svg').resize(tam, tam).png().toFile(`public/icono-${tam}.png`)
  console.log(`public/icono-${tam}.png`)
}
