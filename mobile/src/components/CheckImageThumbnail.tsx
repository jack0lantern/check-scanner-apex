import React from 'react'
import {
  Image,
  TouchableOpacity,
  View,
  StyleSheet,
} from 'react-native'
import { Colors } from '../theme/colors'

interface CheckImageThumbnailProps {
  uri: string
  onPress?: () => void
}

export function CheckImageThumbnail({ uri, onPress }: CheckImageThumbnailProps) {
  const imageSource = uri.startsWith('data:')
    ? { uri }
    : { uri: `data:image/jpeg;base64,${uri}` }

  const content = (
    <Image
      source={imageSource}
      style={styles.image}
      resizeMode="contain"
    />
  )

  if (onPress) {
    return (
      <TouchableOpacity onPress={onPress} activeOpacity={0.8}>
        {content}
      </TouchableOpacity>
    )
  }

  return <View>{content}</View>
}

const styles = StyleSheet.create({
  image: {
    width: '100%',
    height: 160,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: Colors.border,
    backgroundColor: Colors.bgCard,
  },
})
