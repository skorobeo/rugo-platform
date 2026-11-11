const MAX_DIMENSION = 1600
const QUALITY = 0.82

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('image load failed'))
    image.src = src
  })
}

/**
 * Сжимает фото на клиенте (уменьшает до 1600px по большей стороне и перекодирует
 * в JPEG), чтобы не грузить тяжёлые файлы. При любой ошибке возвращает исходный файл.
 */
export async function compressPhoto(file: File): Promise<File> {
  if (!file.type.startsWith('image/')) {
    return file
  }
  let objectUrl: string | null = null
  try {
    objectUrl = URL.createObjectURL(file)
    const image = await loadImage(objectUrl)
    const scale = Math.min(1, MAX_DIMENSION / Math.max(image.width, image.height))
    const width = Math.round(image.width * scale)
    const height = Math.round(image.height * scale)

    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const context = canvas.getContext('2d')
    if (!context) {
      return file
    }
    context.drawImage(image, 0, 0, width, height)

    const blob = await new Promise<Blob | null>((resolve) =>
      canvas.toBlob(resolve, 'image/jpeg', QUALITY),
    )
    if (!blob || blob.size >= file.size) {
      return file
    }
    return new File([blob], file.name.replace(/\.[^.]+$/, '') + '.jpg', { type: 'image/jpeg' })
  } catch {
    return file
  } finally {
    if (objectUrl) {
      URL.revokeObjectURL(objectUrl)
    }
  }
}
