import React from 'react'
import {
  Modal,
  View,
  Image,
  TouchableOpacity,
  Text,
  StyleSheet,
  Dimensions,
} from 'react-native'
import { Colors } from '../theme/colors'

const { width, height } = Dimensions.get('window')

interface FullscreenImageModalProps {
  uri: string | null
  onClose: () => void
}

export function FullscreenImageModal({ uri, onClose }: FullscreenImageModalProps) {
  if (!uri) return null

  const imageSource = uri.startsWith('data:')
    ? { uri }
    : { uri: `data:image/jpeg;base64,${uri}` }

  return (
    <Modal visible={!!uri} transparent animationType="fade">
      <View style={styles.overlay}>
        <TouchableOpacity style={styles.closeButton} onPress={onClose}>
          <Text style={styles.closeText}>Close</Text>
        </TouchableOpacity>
        <Image
          source={imageSource}
          style={styles.image}
          resizeMode="contain"
        />
      </View>
    </Modal>
  )
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.95)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  closeButton: {
    position: 'absolute',
    top: 60,
    right: 20,
    zIndex: 10,
    paddingHorizontal: 16,
    paddingVertical: 8,
    backgroundColor: Colors.bgElevated,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  closeText: {
    color: Colors.text,
    fontSize: 16,
    fontWeight: '600',
  },
  image: {
    width: width - 32,
    height: height * 0.7,
  },
})
